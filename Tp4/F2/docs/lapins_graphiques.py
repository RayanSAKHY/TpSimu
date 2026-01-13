
# -*- coding: utf-8 -*-
"""
Script : lapins_graphiques.py

Objectif
--------
Lire jusqu'à 100 fichiers CSV (ou .txt) au format fourni par Rayan (une ligne
"Timestamp : .." puis un tableau CSV sur 192 lignes / 11 colonnes) et produire
une série de graphiques stylisés semblables aux captures envoyées.

Utilisation
----------
python lapins_graphiques.py --input ./dossier_fichiers --out ./exports

Dépendances : pandas, numpy, matplotlib, seaborn

Le script est robuste aux variations mineures d'en-têtes (accents/espaces) et
sauve toutes les figures en PNG haute résolution dans le dossier --out.
"""

import argparse
import io
import os
import glob
import unicodedata
import re
from datetime import datetime

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from matplotlib.ticker import FuncFormatter

# -----------------------------
# Mise en forme globale (style)
# -----------------------------

def set_style():
    sns.set_theme(context="notebook", style="whitegrid")
    plt.rcParams.update({
        "figure.dpi": 130,
        "savefig.dpi": 150,
        "axes.titlesize": 16,
        "axes.labelsize": 12,
        "legend.fontsize": 11,
        "xtick.labelsize": 10,
        "ytick.labelsize": 10,
        "font.family": "DejaVu Sans",  # gère bien les accents
        "axes.titlepad": 12,
        "axes.grid": True,
        "grid.alpha": 0.25,
        "axes.spines.top": False,
        "axes.spines.right": False,
    })

# Formatage FR pour les ticks (espaces en milliers, virgule décimale)

def fmt_fr(x, pos=None, digits=0):
    try:
        if digits == 0:
            s = f"{int(round(x)):,}"
        else:
            s = f"{x:,.{digits}f}"
        s = s.replace(",", "_")
        s = s.replace(".", ",").replace("_", " ")
        return s
    except Exception:
        return str(x)

fr_tick0 = FuncFormatter(lambda x, p: fmt_fr(x, p, 0))
fr_tick2 = FuncFormatter(lambda x, p: fmt_fr(x, p, 2))

# --------------------------------------
# Lecture & normalisation des fichiers
# --------------------------------------

CANONICAL_MAP = {
    # colonne normalisée : mots-clés possibles (après nettoyage)
    "mois": ["mois"],
    "vivants": ["lapins vivants", "lapin vivants", "nb vivants", "vivants"],
    # cumuls (non utilisés dans tous les graphes mais conservés)
    "morts_juv_cum": [
        "nombre de lapin juveniles morts au total",
        "nb lapin juveniles morts au total",
        "lapin juveniles morts total",
    ],
    "morts_enf_cum": [
        "nombre de lapin enfant mort au total",
        "lapin enfant mort total",
        "nb enfants morts au total",
    ],
    "morts_adult_cum": [
        "nombre de lapin adulte mort au total",
        "lapin adulte mort total",
    ],
    "morts_total_cum": ["total de lapins morts", "total lapins morts"],
    # flux mensuels
    "naiss_mois": ["nombre de naissance par mois", "naissances par mois"],
    "morts_juv_mois": [
        "nombre de lapin juveniles morts par mois",
        "lapin juveniles morts par mois",
    ],
    "morts_enf_mois": [
        "nombre de lapin enfant mort par mois",
        "lapin enfant mort par mois",
    ],
    "morts_adult_mois": [
        "nombre de lapin adulte mort par mois",
        "lapin adulte mort par mois",
    ],
    "morts_mois": [
        "nombre de lapin mort en 1 mois",
        "nombre de lapin mort par mois",
        "morts par mois",
    ],
}

# fonction utilitaire de nettoyage d'étiquette

def clean_label(s: str) -> str:
    s = s.strip().lower()
    s = unicodedata.normalize('NFKD', s)
    s = ''.join(c for c in s if not unicodedata.combining(c))
    s = re.sub(r"\s+", " ", s)
    return s

# retrouver le nom canonique à partir de l'en-tête nettoyé

def map_to_canonical(col: str) -> str:
    c = clean_label(col)
    for canon, options in CANONICAL_MAP.items():
        for opt in options:
            if clean_label(opt) == c:
                return canon
    # heuristique : quelques débuts de chaînes
    if c.startswith("nombre de naissance"):
        return "naiss_mois"
    if c.startswith("nombre de lapin mort en") or c.endswith("mort par mois"):
        return "morts_mois"
    return c  # à défaut on renvoie tel quel


def extract_csv_content(raw: str) -> str:
    """Nettoie le préambule et renvoie uniquement la portion CSV (entête + données).
    - Supprime lignes vides
    - Gère le BOM éventuel
    - Saute les lignes de type 'Timestamp : ...'
    - Repère la ligne d'entête contenant 'Mois' + un séparateur plausible (, ; \t |)
    - Si l'entête n'est pas trouvée, tente de repérer la 1re ligne de données (ex: '1,')
    """
    if not raw or not raw.strip():
        return ""
    lines = raw.splitlines()

    # Remove empty lines and trim
    lines = [ln.rstrip("\n\r") for ln in lines if ln.strip() != ""]

    if not lines:
        return ""

    # Remove BOM on first non-empty line and normalize
    lines[0] = lines[0].lstrip("\ufeff")

    # Drop 'Timestamp : ...' and autres préambules jusqu'à détecter un header
    def _clean_label(s: str) -> str:
        s = s.strip().lower()
        s = unicodedata.normalize('NFKD', s)
        s = ''.join(c for c in s if not unicodedata.combining(c))
        s = re.sub(r"\s+", " ", s)
        return s

    header_idx = None
    expected_token = "mois"
    seps = [",", ";", "\t", "|"]

    for i, ln in enumerate(lines):
        lnc = _clean_label(ln)
        if lnc.startswith("timestamp"):
            # ignorer la ligne de timestamp et continuer
            continue
        # repérage d'un header plausible : contient "mois" + un séparateur
        if expected_token in lnc and any(sep in ln for sep in seps):
            header_idx = i
            break

    # fallback : si pas de header explicite trouvé, on tente ligne de données style "1,<...>"
    if header_idx is None:
        for i, ln in enumerate(lines):
            if re.match(r"\s*\d+\s*[,;\t|]", ln):
                header_idx = max(i - 1, 0)  # on suppose l'entête juste au-dessus
                break

    if header_idx is None:
        # rien d'exploitable
        return ""

    content = "\n".join(lines[header_idx:])
    return content


def read_one_file(path: str) -> pd.DataFrame:
    """Lit un fichier, nettoie le préambule, infère le séparateur et normalise les colonnes.
       Lève une exception explicite si le contenu exploitable est vide.
    """
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        raw = f.read()

    content = extract_csv_content(raw)
    if not content.strip():
        raise ValueError("Fichier vide ou sans entête/données exploitables après nettoyage.")

    # sep=None => inférence automatique (moteur Python requis)
    df = pd.read_csv(io.StringIO(content), sep=None, engine="python")

    # ---- mapping colonnes (inchangé) ----
    new_cols = {}
    for col in df.columns:
        canon = map_to_canonical(col)
        new_cols[col] = canon
    df = df.rename(columns=new_cols)

    keep = [
        "mois", "vivants", "naiss_mois", "morts_mois",
        "morts_juv_mois", "morts_enf_mois", "morts_adult_mois",
        "morts_total_cum", "morts_juv_cum", "morts_enf_cum", "morts_adult_cum"
    ]
    for k in keep:
        if k not in df.columns:
            df[k] = np.nan

    if "mois" not in df.columns:
        raise ValueError("Colonne 'Mois' introuvable après normalisation.")

    df["mois"] = pd.to_numeric(df["mois"], errors="coerce")
    # conversions numériques
    for k in keep:
        if k == "mois":
            continue
        df[k] = pd.to_numeric(df[k], errors="coerce")

    # borne sur 1..192 (tu peux assouplir si besoin)
    df = df[(df["mois"] >= 1) & (df["mois"] <= 192)].copy()
    df["source_file"] = os.path.basename(path)
    if df.empty:
        raise ValueError("Aucune ligne 1..192 trouvée (fichier peut-être vide après filtrage).")
    return df


def load_folder(folder: str, limit: int = 100) -> pd.DataFrame:
    """Charge jusqu'à 100 fichiers csv/txt, en ignorant silencieusement
       ceux qui sont vides ou mal formés, tout en loggant un avertissement.
    """
    paths = []
    for ext in ("*.csv", "*.txt"):
        paths.extend(glob.glob(os.path.join(folder, ext)))
    paths = sorted(paths)[:limit]

    if not paths:
        raise FileNotFoundError(f"Aucun fichier .csv ou .txt trouvé dans {folder}.")

    frames = []
    skipped = []
    for p in paths:
        try:
            # ignore les fichiers de taille 0 (ou quasi)
            if os.path.getsize(p) < 8:
                raise ValueError("Fichier trop petit (probablement vide).")
            df = read_one_file(p)
            frames.append(df)
        except Exception as e:
            print(f"[AVERTISSEMENT] Fichier ignoré: {os.path.basename(p)} → {e}")
            skipped.append((p, str(e)))

    if not frames:
        # on relance une erreur plus parlante avec la liste des fichiers en cause
        raise RuntimeError(
            "Aucun fichier valide n'a été chargé. "
            f"{len(skipped)} fichier(s) ignoré(s). Vérifie le séparateur, l'entête et les lignes de timestamp."
        )

    df_all = pd.concat(frames, ignore_index=True)
    if skipped:
        print(f"[INFO] {len(skipped)} fichier(s) ignoré(s). Traitement poursuivi avec {len(frames)} fichier(s).")
    return df_all



def read_csv_smart(content: str) -> pd.DataFrame:
    """Essaye plusieurs séparateurs pour lire le tableau."""
    for sep in (None, ",", ";", "\t", "|"):
        try:
            df = pd.read_csv(io.StringIO(content), sep=sep, engine="python")
            # Il faut au moins l'entête + quelques colonnes
            if df.shape[1] >= 5:
                return df
        except Exception:
            pass
    raise ValueError("Impossible d'inférer le séparateur (essayé: auto, ',', ';', '\\t', '|').")


def read_one_file(path: str) -> pd.DataFrame:
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        raw = f.read()

    content = extract_csv_content(raw)  # ta fonction de nettoyage
    if not content.strip():
        raise ValueError("Fichier vide ou sans entête/données exploitables après nettoyage.")

    # Lecture robuste
    df = read_csv_smart(content)

    # Normalisation des colonnes (inchangé)
    new_cols = {}
    for col in df.columns:
        canon = map_to_canonical(col)
        new_cols[col] = canon
    df = df.rename(columns=new_cols)

    # Colonnes attendues
    keep = [
        "mois", "vivants", "naiss_mois", "morts_mois",
        "morts_juv_mois", "morts_enf_mois", "morts_adult_mois",
        "morts_total_cum", "morts_juv_cum", "morts_enf_cum", "morts_adult_cum"
    ]
    for k in keep:
        if k not in df.columns:
            df[k] = np.nan

    # Conversions numériques
    if "mois" not in df.columns:
        raise ValueError("Colonne 'Mois' introuvable après normalisation.")
    df["mois"] = pd.to_numeric(df["mois"], errors="coerce")
    for k in keep:
        if k == "mois":
            continue
        df[k] = pd.to_numeric(df[k], errors="coerce")

    # Filtre 1..192
    df = df[(df["mois"] >= 1) & (df["mois"] <= 192)].copy()
    df["source_file"] = os.path.basename(path)

    # Sanity‑check : au moins une des colonnes clés doit être non‑nulle
    required = ["naiss_mois", "morts_mois", "vivants"]
    nn = {c: int(df[c].notna().sum()) for c in required}
    if min(nn.values()) == 0:
        raise ValueError(f"Données vides pour {path}. Non‑nuls: {nn}. "
                         "Vérifie séparateur/entête/format des nombres.")

    return df



def load_folder(folder: str, limit: int = 100) -> pd.DataFrame:
    paths = []
    for ext in ("*.csv", "*.txt"):
        paths.extend(glob.glob(os.path.join(folder, ext)))
    paths = sorted(paths)[:limit]
    if not paths:
        raise FileNotFoundError(
            f"Aucun fichier .csv ou .txt trouvé dans {folder}."
        )
    all_df = [read_one_file(p) for p in paths]
    df = pd.concat(all_df, ignore_index=True)
    return df

# ----------------------------
# Agrégations statistique
# ----------------------------

METRICS = {
    "vivants": "Lapins vivants",
    "naiss_mois": "Nombre de naissance par mois",
    "morts_mois": "Nombre de lapin mort en 1 mois",
    "morts_juv_mois": "Décès juvéniles par mois",
    "morts_enf_mois": "Décès non matures par mois",
    "morts_adult_mois": "Décès matures par mois",
}


def stats_by_month(df: pd.DataFrame):
    # moyenne / médiane / écart-type par mois (sur l'échantillon de fichiers)
    grp = df.groupby("mois")
    mean = grp.mean(numeric_only=True)[list(METRICS.keys())]
    med = grp.median(numeric_only=True)[list(METRICS.keys())]
    std = grp.std(numeric_only=True, ddof=0)[list(METRICS.keys())]
    count = grp.size().rename("n_fichiers").to_frame()
    return mean, med, std, count


def values_by_tranche(df: pd.DataFrame, metric: str):
    """Retourne une liste [vals_1_4, vals_5_8, vals_9_12, vals_13_16]
    où chaque élément est un vecteur de valeurs (tous fichiers confondus)
    pour les mois de la tranche.
    """
    tranches = [(1, 48), (49, 96), (97, 144), (145, 192)]
    res = []
    for a, b in tranches:
        vals = df[(df["mois"] >= a) & (df["mois"] <= b)][metric].dropna().values
        res.append(vals)
    return res


# ----------------------------
# Fonctions de tracé
# ----------------------------

PALETTE = {
    "naiss": "#1f77b4",   # bleu
    "morts": "#d62728",   # rouge
    "delta": "#2ca02c",   # vert
    "vivants": "#4c78a8",
    "band": "#9ecae1",
    "juv": "#1f77b4",
    "enf": "#ff7f0e",
    "adult": "#2ca02c",
}


def savefig(fig, out_dir, name):
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, name)
    fig.tight_layout()
    fig.savefig(path, bbox_inches="tight")
    plt.close(fig)
    return path


def plot_boxplots_tranches(df_all: pd.DataFrame, out_dir: str):
    labels = ["Années 1–4", "Années 5–8", "Années 9–12", "Années 13–16"]
    metr_to_title = {
        "morts_mois": ("Morts mensuels", "Morts par mois"),
        "naiss_mois": ("Naissances", "Naissances par mois"),
        "vivants": ("Vivants", "Lapins vivants"),
    }
    for m, (title_suf, ylab) in metr_to_title.items():
        data = values_by_tranche(df_all, m)
        if all((len(d) == 0) for d in data):    
            print(f"[INFO] Boxplot {m}: toutes les tranches vides → figure ignorée.")    
            return
        else: fig, ax = plt.subplots(figsize=(12, 8))
        bp = ax.boxplot(data, patch_artist=True, tick_labels=labels)
        # couleur par métrique
        color = {
            "morts_mois": "#f28e8c",
            "naiss_mois": "#9fe3b0",
            "vivants": "#b3c6e6",
        }[m]
        for patch in bp['boxes']:
            patch.set(facecolor=color, alpha=0.6)
            patch.set(edgecolor="#b54e4e" if m == "morts_mois" else "#5b8a72")
        ax.set_title(f"Variabilité par tranches (boîtes à moustaches) – {title_suf}")
        ax.set_ylabel(ylab)
        ax.yaxis.set_major_formatter(fr_tick0)
        savefig(fig, out_dir, f"boxplot_{m}_tranches.png")


def compute_yearly(df_all: pd.DataFrame):
    # agrège par fichier puis par année, puis moyenne sur les fichiers
    df = df_all.copy()
    df["annee"] = ((df["mois"] - 1) // 12 + 1).astype(int)
    yearly = df.groupby(["source_file", "annee"]).agg({
        "naiss_mois": "sum",
        "morts_mois": "sum",
        "vivants": "mean",  # moy. vivants sur l'année
        "morts_juv_mois": "sum",
        "morts_enf_mois": "sum",
        "morts_adult_mois": "sum",
    }).reset_index()
    mean_yearly = yearly.groupby("annee").mean(numeric_only=True)
    std_yearly = yearly.groupby("annee").std(numeric_only=True, ddof=0)
    return mean_yearly, std_yearly, yearly


def annotate_bar_values(ax, fmt="{:.0f}"):
    for p in ax.patches:
        height = p.get_height()
        if np.isnan(height):
            continue
        if height >= 0:
            ax.annotate(fmt.format(height),
                        (p.get_x() + p.get_width()/2, height),
                        ha='center', va='bottom', fontsize=9, rotation=0)
        else:
            ax.annotate(fmt.format(height),
                        (p.get_x() + p.get_width()/2, height),
                        ha='center', va='top', fontsize=9, rotation=0)



def plot_waterfall(mean_yearly: pd.DataFrame, out_dir: str, first4: bool = False):
    df = mean_yearly.copy()
    if first4:
        df = df.loc[df.index <= 4]
        title = "Cascade annuelle sur les 4 premières années"
        fname = "graphe_cascade1-4.png"
    else:
        title = "Cascade annuelle"
        fname = "graphe_cascade1-16.png"

    # Garde-fou : si données vides, on ne trace pas
    if df.empty or df[["naiss_mois", "morts_mois"]].dropna().empty:
        print("[INFO] Cascade: données vides → figure ignorée.")
        return

    # 1) Barres POSITIVES (au-dessus de l'axe)
    cat, val, colors = [], [], []
    for y in df.index:
        # Naissances : barre positive
        cat.append(f"Année {y} Naissances")
        val.append(df.loc[y, "naiss_mois"])
        colors.append(PALETTE["naiss"])
        # Morts : barre positive (mais sera soustraite dans le cumul)
        cat.append(f"Année {y} Morts")
        val.append(df.loc[y, "morts_mois"])
        colors.append(PALETTE["morts"])

    x = range(len(cat))
    fig, ax = plt.subplots(figsize=(14, 6))
    ax.bar(x, val, color=colors)


    # Axes/labels
    ax.set_xticks(x)
    ax.set_xticklabels(cat, rotation=60, ha='right')
    ax.set_title(title)
    ax.set_ylabel("Nombre")
    ax.yaxis.set_major_formatter(fr_tick0)
    ax.legend(title="Légende", loc="upper left")

    # (optionnel) Annotation des barres
    for i, h in enumerate(val):
        if not np.isnan(h):
            ax.annotate(fmt_fr(h), (i, h), ha='center', va='bottom', fontsize=9)

    savefig(fig, out_dir, fname)



def plot_solde_par_an(mean_yearly: pd.DataFrame, out_dir: str):
    df = mean_yearly.copy()
    df["solde"] = df["naiss_mois"] - df["morts_mois"]
    fig, ax = plt.subplots(figsize=(14, 6))
    bars = ax.bar([f"Année {i} Solde" for i in df.index], df["solde"], color=PALETTE["naiss"], label="Hausse")
    ax.set_title("Nombre de lapin par an")
    ax.set_ylabel("Nombre")
    ax.set_xticklabels(ax.get_xticklabels(), rotation=60, ha='right')
    ax.yaxis.set_major_formatter(fr_tick0)
    for b in bars:
        ax.annotate(fmt_fr(b.get_height()), (b.get_x()+b.get_width()/2, b.get_height()),
                    ha='center', va='bottom', fontsize=9)
    savefig(fig, out_dir, "graphe_cascadeSolde.png")


def plot_proportion_juvenile(df_mean: pd.DataFrame, out_dir: str):
    eps = 1e-9
    prop = (df_mean["morts_juv_mois"] / (df_mean["morts_mois"] + eps) * 100.0).clip(lower=0)
    fig, ax = plt.subplots(figsize=(16, 5))
    ax.plot(df_mean.index, prop, color="#205375", linewidth=2.5)
    ax.set_title("Proportion de mort juvénile par rapport au nombre total de lapins morts en fonction du temps")
    ax.set_xlabel("Temps (en mois)")
    ax.set_ylabel("Pourcentage")
    ax.set_xlim(1, 192)
    ax.yaxis.set_major_formatter(fr_tick2)
    ax.xaxis.set_major_locator(plt.MaxNLocator(24))
    savefig(fig, out_dir, "GrapheLapinJuvenileMortProportion.png")


def plot_hist_abs(df_mean: pd.DataFrame, out_dir: str):
    fig, ax = plt.subplots(figsize=(16, 6))
    x = df_mean.index
    juv = df_mean["morts_juv_mois"].fillna(0)
    enf = df_mean["morts_enf_mois"].fillna(0)
    adu = df_mean["morts_adult_mois"].fillna(0)
    ax.bar(x, juv, color=PALETTE["juv"], label="Nombre de lapins morts juvénile par mois en moyenne")
    ax.bar(x, enf, bottom=juv, color=PALETTE["enf"], label="Nombre de lapins morts non mature par mois en moyenne")
    ax.bar(x, adu, bottom=juv+enf, color=PALETTE["adult"], label="Nombre de lapins morts mature par mois en moyenne")
    ax.set_title("Structure des décès par âge (valeurs absolues) chaque mois")
    ax.set_xlabel("Temps (en mois)")
    ax.set_ylabel("Nombre")
    ax.set_xlim(1, 192)
    ax.yaxis.set_major_formatter(fr_tick0)
    ax.legend(loc="upper left")
    savefig(fig, out_dir, "graphe_histogrammeEmpile.png")


def plot_hist_100(df_mean: pd.DataFrame, out_dir: str):
    fig, ax = plt.subplots(figsize=(16, 6))
    x = df_mean.index
    juv = df_mean["morts_juv_mois"].fillna(0)
    enf = df_mean["morts_enf_mois"].fillna(0)
    adu = df_mean["morts_adult_mois"].fillna(0)
    tot = (juv + enf + adu).replace(0, np.nan)
    ax.bar(x, juv / tot * 100, color=PALETTE["juv"], label="Nombre de mort infantiles chez les lapins en moyenne")
    ax.bar(x, enf / tot * 100, bottom=juv / tot * 100, color=PALETTE["enf"], label="Nombre de lapins morts non mature en moyenne")
    ax.bar(x, adu / tot * 100, bottom=(juv + enf) / tot * 100, color=PALETTE["adult"], label="Nombre de lapins morts mature en moyenne")
    ax.set_title("Structure des décès par âge (valeurs absolues)")
    ax.set_xlabel("Temps (en mois)")
    ax.set_ylabel("Pourcentage de lapins morts")
    ax.set_xlim(1, 192)
    ax.yaxis.set_major_formatter(fr_tick0)
    ax.set_ylim(0, 100)
    ax.legend(loc="lower left", ncol=1)
    savefig(fig, out_dir, "graphe_histogramme100.png")



def plot_delta(df_mean: pd.DataFrame, out_dir: str):
    x = df_mean.index
    naiss = df_mean["naiss_mois"].fillna(0)
    morts = df_mean["morts_mois"].fillna(0)
    delta = naiss - morts

    fig, ax1 = plt.subplots(figsize=(16, 6))

    # --- Lignes sur l'axe principal (devant) ---
    ax1.plot(x, naiss, color=PALETTE["naiss"], linewidth=2.6, label="Nombre de naissance par mois en moyenne", zorder=4)
    ax1.plot(x, morts, color=PALETTE["morts"], linewidth=2.4, label="Nombre de mort par mois en moyenne", zorder=3)

    ax1.set_xlabel("Temps (en mois)")
    ax1.set_ylabel("Nombre de lapins")
    ax1.yaxis.set_major_formatter(fr_tick0)
    ax1.legend(loc="upper left")

    # --- Axe secondaire pour Δ ---
    ax2 = ax1.twinx()
    # Style moins intrusif pour le Δ (pour ne pas recouvrir le bleu)
    ax2.plot(x, delta, color=PALETTE["delta"], linewidth=2.2, linestyle="--", alpha=0.75, label="Delta par mois", zorder=2)
    ax2.set_ylabel("Nombre de lapins")
    ax2.yaxis.set_major_formatter(fr_tick0)
    ax2.legend(loc="upper right")

    # --- Assure que les courbes ax1 sont au-dessus de ax2 ---
    ax1.set_zorder(3)
    ax2.set_zorder(2)
    # Pour éviter que le fond de ax1 masque ax2 (ou l’inverse)
    ax1.patch.set_visible(False)

    # (optionnel) borne inférieure à 0 pour mieux voir les lignes au-dessus de l’axe
    ax1.set_ylim(bottom=0)

    ax1.set_title("Naissances vs Morts par mois et Croissance nette (Δ)")
    savefig(fig, out_dir, "graphe_Delta.png")



def plot_morts_std_vs_mean(df_mean: pd.DataFrame, df_std: pd.DataFrame, out_dir: str):
    fig, ax = plt.subplots(figsize=(16, 6))
    m = df_mean["morts_mois"].fillna(0)
    s = df_std["morts_mois"].fillna(0)
    ax.scatter(m, s, s=28, color="#0c5c78")
    ax.set_title("Ecart type du nombre de mort par mois")
    ax.set_xlabel("Moyenne des morts par mois")
    ax.set_ylabel("Ecart type du nombre de mort par mois")
    ax.xaxis.set_major_formatter(fr_tick0)
    ax.yaxis.set_major_formatter(fr_tick0)
    savefig(fig, out_dir, "grapheLapinMortParMoisEcartType.png")


def plot_vivants_mean_std(df_mean: pd.DataFrame, df_std: pd.DataFrame, out_dir: str):
    fig, ax = plt.subplots(figsize=(16, 6))
    x = df_mean.index
    ax.plot(x, df_mean["vivants"], color=PALETTE["vivants"], label="Nombre de lapin vivants en moyenne", linewidth=2.4)
    ax.plot(x, df_std["vivants"], color="#f28e2b", label="Ecart type du nombre de lapins vivants", linewidth=2.0)
    ax.set_title("Nombre de lapin vivants en fonction du temps")
    ax.set_xlabel("Temps (en mois)")
    ax.set_ylabel("Nombre")
    ax.xaxis.set_major_locator(plt.MaxNLocator(24))
    ax.yaxis.set_major_formatter(fr_tick0)
    ax.legend(loc="upper left")
    savefig(fig, out_dir, "grapheLapinVivantMoyenne.png")


def plot_mean_median_band(df_mean: pd.DataFrame, df_med: pd.DataFrame, df_std: pd.DataFrame, out_dir: str):
    x = df_mean.index
    mu = df_mean["vivants"].values
    md = df_med["vivants"].values
    sd = df_std["vivants"].values
    fig, ax = plt.subplots(figsize=(16, 9))
    ax.fill_between(x, mu - sd, mu + sd, color=PALETTE["band"], alpha=0.35, label="Bande ± Écart-type")
    ax.plot(x, mu, color=PALETTE["vivants"], linewidth=2.5, label="Moyenne")
    ax.plot(x, md, color=PALETTE["vivants"], linestyle='--', linewidth=2.0, label="Médiane")
    ax.set_title("Moyenne vs Médiane avec bande ± Écart-type – Vivants (1–192 mois)")
    ax.set_xlabel("Mois")
    ax.set_ylabel("Lapins vivants")
    ax.yaxis.set_major_formatter(fr_tick0)
    ax.legend(loc="upper left")
    savefig(fig, out_dir, "moy_med_et_vivants.png")


def plot_courbes_proportions(df_mean: pd.DataFrame, out_dir: str):
    eps = 1e-9
    juv = df_mean["morts_juv_mois"].fillna(0)
    enf = df_mean["morts_enf_mois"].fillna(0)
    adu = df_mean["morts_adult_mois"].fillna(0)
    tot = juv + enf + adu + eps
    fig, ax = plt.subplots(figsize=(16, 6))
    x = df_mean.index
    ax.plot(x, juv / tot * 100, color=PALETTE["juv"], label="Proportion de lapin juvénile par rapport au nombre total de lapins morts")
    ax.plot(x, enf / tot * 100, color=PALETTE["enf"], label="Proportion de lapin non mature par rapport au nombre total de lapins morts")
    ax.plot(x, adu / tot * 100, color=PALETTE["adult"], label="Proportion de lapin mature par rapport au nombre total de lapins morts")
    ax.set_title("Évolution mensuelle des proportions de décès par catégorie d’âge")
    ax.set_xlabel("Temps (en mois)")
    ax.set_ylabel("Pourcentage de lapin mort")
    ax.set_xlim(1, 192)
    ax.set_ylim(0, 100)
    ax.yaxis.set_major_formatter(fr_tick0)
    ax.legend(loc="lower right")
    savefig(fig, out_dir, "graphe_courbesProportions.png")


def plot_dynamiques(df_mean: pd.DataFrame, out_dir: str):
    # Relation morts cumulées vs naissances cumulées
    mort_cum = df_mean["morts_mois"].fillna(0).cumsum()
    naiss_cum = df_mean["naiss_mois"].fillna(0).cumsum()
    fig, ax = plt.subplots(figsize=(14, 6))
    ax.plot(naiss_cum, mort_cum, color="#fb8500")
    ax.set_title("Nombre de lapin morts total en moyenne en fonction du nombre de naissance")
    ax.set_xlabel("Naissances cumulées (moyenne)")
    ax.set_ylabel("Morts cumulées (moyenne)")
    ax.xaxis.set_major_formatter(fr_tick0)
    ax.yaxis.set_major_formatter(fr_tick0)
    savefig(fig, out_dir, "graphe_dynamiqueGlob.png")

# ----------------------------
# Export des données agrégées
# ----------------------------


def export_tables(df_all: pd.DataFrame, df_mean: pd.DataFrame, df_med: pd.DataFrame, df_std: pd.DataFrame, out_dir: str):
    # table brute concaténée
    ts = datetime.now().strftime("%d|%m_%Hh%M")
    raw_path = os.path.join(out_dir, f"donnees_brutes_concat_{ts}.csv")
    df_all.to_csv(raw_path, index=False)
    # stats
    stats = df_mean.copy()
    stats.columns = [f"moy_{c}" for c in stats.columns]
    stats["med_vivants"] = df_med["vivants"]
    stats["std_vivants"] = df_std["vivants"]
    stats_path = os.path.join(out_dir, f"stats_mensuelles_{ts}.csv")
    stats.to_csv(stats_path)
    return raw_path, stats_path

# ----------------------------
# Main
# ----------------------------


def main():
    parser = argparse.ArgumentParser(description="Générer des graphiques à partir de simulations de population de lapins.")
    parser.add_argument("--input", required=True, help="Dossier contenant jusqu'à 100 fichiers CSV/TXT")
    parser.add_argument("--out", required=False, default="images", help="Dossier de sortie pour les images")
    args = parser.parse_args()

    set_style()

    df_all = load_folder(args.input, limit=100)

    # petit snippet à insérer temporairement après load_folder(...)
    sample_file = next(iter(sorted(glob.glob(os.path.join(args.input, "*.csv")))), None)
    if sample_file:
        df_test = read_one_file(sample_file)
        print("Colonnes normalisées:", list(df_test.columns))
        print("Non-nuls:", {c: int(df_test[c].notna().sum()) for c in ["mois","naiss_mois","morts_mois","vivants"]})
        print(df_test.head(5))

    # calculs statistiques
    df_mean, df_med, df_std, df_count = stats_by_month(df_all)

    # Création des graphes
    plot_boxplots_tranches(df_all, args.out)
    mean_yearly, std_yearly, yearly = compute_yearly(df_all)
    plot_waterfall(mean_yearly, args.out, first4=True)
    plot_waterfall(mean_yearly, args.out, first4=False)
    plot_solde_par_an(mean_yearly, args.out)
    plot_proportion_juvenile(df_mean, args.out)
    plot_hist_abs(df_mean, args.out)
    plot_delta(df_mean, args.out)
    plot_dynamiques(df_mean, args.out)
    plot_morts_std_vs_mean(df_mean, df_std, args.out)
    plot_vivants_mean_std(df_mean, df_std, args.out)
    plot_hist_100(df_mean, args.out)
    plot_mean_median_band(df_mean, df_med, df_std, args.out)
    plot_courbes_proportions(df_mean, args.out)

    raw_csv, stats_csv = export_tables(df_all, df_mean, df_med, df_std, args.out)
    print(f"Export des données : - Données brutes concaténées : {raw_csv} - Statistiques mensuelles     : {stats_csv}")
    print(f"Figures sauvegardées dans : {os.path.abspath(args.out)}")


if __name__ == "__main__":
    main()

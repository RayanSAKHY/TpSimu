% isima-report.cls — classe LaTeX minimaliste pour rapports ISIMA
\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{isima-report}[2025/12/06 ISIMA report class]

% Options par défaut
\DeclareOption*{\PassOptionsToClass{\CurrentOption}{article}}
\ProcessOptions\relax
\LoadClass[12pt,a4paper]{article}

% Packages de base
\RequirePackage[utf8]{inputenc}
\RequirePackage[T1]{fontenc}
\RequirePackage[french]{babel}
\RequirePackage{geometry}
\geometry{margin=25mm}
\RequirePackage{newtxtext,newtxmath} % police type Times
\RequirePackage{graphicx}
\RequirePackage{xcolor}
\RequirePackage{hyperref}
\hypersetup{colorlinks=true, linkcolor=blue!60!black, urlcolor=blue!60!black, citecolor=blue!60!black}
\RequirePackage{fancyhdr}
\RequirePackage{titlesec}
\RequirePackage{enumitem}
\RequirePackage{listings}
\RequirePackage{cleveref}
\RequirePackage{algorithm}
\RequirePackage{algpseudocode}
% --- En-têtes/pieds de page ---
\RequirePackage{fancyhdr}
\RequirePackage{lastpage} % pour X/Total

% Index (permet des index nommés)
\RequirePackage{imakeidx}
\makeindex[name=code,title=Index du code] % index dédié aux extraits de code


% Couleurs
\definecolor{isimaRed}{RGB}{196,0,26}
\definecolor{codegray}{RGB}{245,245,245}
\definecolor{darkgray}{RGB}{80,80,80}

\renewcommand{\lstlistlistingname}{Index des codes}

% Listings Java
\lstdefinestyle{java}{
  language=Java,
  basicstyle=\ttfamily\small,
  backgroundcolor=\color{codegray},
  keywordstyle=\color{isimaRed},
  commentstyle=\color{darkgray},
  stringstyle=\color{blue!60!black},
  showstringspaces=false,
  columns=fullflexible,
  breaklines=true
}

% Réglages globaux des listings
\lstset{
  float,                 % rend lstlisting "flottant" (comme figure)
  captionpos=b,          % légende en bas
  numbers=left,          % numéros de ligne à gauche
  numberstyle=\tiny,
  numbersep=6pt,
  breaklines=true,
  frame=single,
  basicstyle=\ttfamily\small
}


% Entêtes/pieds de page
\pagestyle{fancy}
\fancyhf{}
\lhead{\Course}
\rhead{\ShortTitle}
% gauche = Auteur, centre = page/Total, droite = ISIMA
\fancyfoot{} % on part d'un pied vierge
\fancyfoot[L]{\AuthorName}
\fancyfoot[C]{\thepage/\pageref{LastPage}}
\fancyfoot[R]{\Institution}
%\renewcommand{\footrulewidth}{0.4pt} % petite règle au pied (optionnel)

% Macros d'identité
\newcommand{\Institution}{ISIMA}
\newcommand{\AuthorName}{Nom Prénom}
\newcommand{\AuthorEmail}{prenom.nom@etu.isima.fr}
\newcommand{\Course}{Unité d'enseignement}
\newcommand{\ReportTitle}{Titre du rapport}
\newcommand{\ShortTitle}{Titre court}
\newcommand{\ReportSubtitle}{Sous-titre}
\newcommand{\ReportDate}{Janvier 2025}

% Page de garde
\newcommand{\maketitlepage}{%
  \begin{titlepage}
    \vspace*{2cm}
    {\Huge\bfseries \ReportTitle}\par
    \vspace{0.5cm}
    {\Large \ReportSubtitle}\par
    \vspace{1.5cm}
    {\large \textbf{\Institution}}\par
    \vspace{0.2cm}
    {\large \Course}\par
    \vspace{1.5cm}
    \begin{tabular}{ll}
      \textbf{Auteur :} & \AuthorName \\
      \textbf{Email :} & \AuthorEmail \\
      \textbf{Date :} & \ReportDate \\
    \end{tabular}
    \vfill
    \noindent\rule{\textwidth}{0.4pt}\\[2mm]
    {\small Ce document est réalisé dans le cadre de la formation ISIMA.}
  \end{titlepage}
}

% Styles de section
\titleformat{\section}{\Large\bfseries\color{isimaRed}}{\thesection}{1em}{}
\titleformat{\subsection}{\large\bfseries}{\thesubsection}{1em}{}
\titleformat{\subsubsection}{\normalsize\bfseries}{\thesubsubsection}{1em}{}

\usepackage[most]{tcolorbox}
\tcbset{colback=white, colframe=isimaRed, boxrule=0.8pt, arc=1pt}

\newtcolorbox{objectifbox}[1]{title=#1, fonttitle=\bfseries, left=6pt, right=6pt, top=6pt, bottom=6pt, width=0.95\textwidth, enhanced, center title}

% Raccourci avec titre par défaut
\newenvironment{objectif}{\begin{objectifbox}{Objectifs}}{\end{objectifbox}}


% Permet d'avoir une "Table des annexes" basée sur les listings
\renewcommand{\lstlistlistingname}{Table des annexes}

% --- Figures & légendes ---
\usepackage{graphicx}
\usepackage{caption}
\captionsetup[figure]{name={Figure},labelfont=bf}

% --- Tableaux ---
\usepackage{array}
\usepackage{booktabs}

% Fin de la classe

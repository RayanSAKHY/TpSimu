package ISIMA.F2.assets;

/**
 * Création des lapins avec la taille minimum.
 */
public class Lapin {
  /**
   * Stocke l'âge du lapin en Mois.
   */
  public int age;
  /**
   * Stocke le sexe du lapin avec comme valeur 1 pour les femmelles et 0 pour les males.
   */
  public int sexe;
  /**
   * Stocke l'information de la maturité du lapin.
   */
  public boolean estMature;
  /**
   * Stocke l'age à partir duquel le lapin sera mature.
   */
  public int ageMaturite;
  /**
   * Stocke le nombre de portee restante pour toutes les lapines.
   */
  public int nbPorteeRestante;
  /**
   * Stocke le nombre d'enfants que les lapines portent.
   */
  public int nbEnfantEnGestation;

  /**
   *Cette fonction instancie un nouveau lapin.
   *
   * @param age : l'âge du lapin
   * @param sexe : le sexe du nouveau lapin
   * @param ageMaturite : l'age auquel le lapin sera mature
   *
   */
  public Lapin(int age, int sexe, int ageMaturite) {
    this.age = age;
    this.sexe = sexe;
    nbPorteeRestante = 0;
    this.ageMaturite = ageMaturite;
    nbEnfantEnGestation = 0;
    if (age >= ageMaturite) {
      estMature = true;
    }
  }
}
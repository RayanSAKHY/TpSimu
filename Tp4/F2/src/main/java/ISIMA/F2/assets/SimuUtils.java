package ISIMA.F2.assets;

import ISIMA.F2.LoggerCsv;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.commons.math3.random.MersenneTwister;

/**
 * Implementation de toutes les fonctions pour gérer la simulation.
 */
public class SimuUtils {
  /**
   * Suivi du nombre de lapins total qui sont né durant toute la simulation.
   */
  private int nbLapinTot = 0;

  /**
   * Suivi du nombre de lapins total qui sont né durant un mois.
   */
  private int nbLapin = 0;

  /**
   * Suivi du nombre de lapins de moins de 1 mois total qui sont mort durant la simulation.
   */
  private int nbBebeMortTot = 0;
  /**
   * Suivi du nombre de lapins de moins de 1 mois qui sont mort au cours d'un mois.
   */
  private int nbBebeMort = 0;

  /**
   * Suivi du nombre de lapins pas encore mature total qui sont mort durant la simulation.
   */
  private int nbEnfantMortTot = 0;

  /**
   * Suivi du nombre de lapins non mature qui sont mort au cours d'un mois.
   */
  private int nbEnfantMort = 0;

  /**
   * Suivi du nombre de lapins total qui sont mort durant la simulation.
   */
  private int nbLapinMortTot = 0;

  /**
   * Suivi du nombre de lapins total qui sont mort durant un mois.
   */
  private int nbLapinMort = 0;

  /**
   * Suivi du nombre de lapins mature qui sont mort au cours d'un mois.
   */
  private int nbAdulteMortTot = 0;

  /**
   * Suivi du nombre de lapins de plus de 8 ans total qui sont mort durant la simulation.
   */
  private int nbAdulteMort = 0;

  /**
   * * Âge maximal considéré pour le calcul des probabilités de mortalité (en mois).
   */
  private static final int MAX_AGE = 180;
  /**
   * Table des probabilités de mortalité pour les lapins matures selon le profil V2 (sauvage).
   */
  private static final double[] hazardMatureV2 = new double[MAX_AGE + 1];
  /**
   * Table des probabilités de mortalité pour les lapins non matures selon le profil V2 (sauvage).
   */
  private static final double[] hazardNonMatureV2 = new double[MAX_AGE + 1];
  /**
   * Table des probabilités de mortalité pour les lapins matures selon le profil V1 (domestique).
   */
  private static final double[] hazardMatureV1 = new double[MAX_AGE + 1];
  /**
   * Table des probabilités de mortalité pour les lapins non matures selon le profil 1 (domestique).
   */
  private static final double[] hazardNonMatureV1 = new double[MAX_AGE + 1];

  static {
    initHazards();
  }
  /**
   * Initialise les tables de probabilités de mortalité pour tous les âges et profils.
   * Cette méthode doit être appelée une seule fois au démarrage de la simulation.
   * Les tableaux sont ensuite utilisés en lecture seule.
   */

  private static void initHazards() {
    for (int age = 0; age <= MAX_AGE; age++) {

      hazardMatureV1[age]    = (age == 180) ? 1.0
              : (age >= 168) ? 0.126
              : (age >= 156) ? 0.0997
              : (age >= 144) ? 0.0847
              : (age >= 132) ? 0.0698
              : (age >= 120) ? 0.0561
              : 0.0426;
      hazardNonMatureV1[age] = 0.0649;

      hazardMatureV2[age]    = (age == 180) ? 1.0
              : (age >= 168) ? 0.126
              : (age >= 156) ? 0.0997
              : (age >= 144) ? 0.0847
              : (age >= 132) ? 0.0698
              : (age >= 120) ? 0.0561
              : (age >= 108) ? 0.0426
              : (age >= 96)  ? 0.0296
              : 0.0184;
      hazardNonMatureV2[age] = (age < 1) ? 0.8 : 0.0561;
    }
  }

  /**
   *Cette fonction sert à simuler des tirages aléatoires avec des probabilités donnés en argument.
   *
   * @param poids : Tableau qui stocke la pondération voulue pour le tirage aléatoire
   * @param rng : Instanciation de Mersenne Twister
   * @return : Renvoie l'index qui correspond à la probabilité
   */
  private static int tirageProbaSelonPoids(double[] poids, MersenneTwister rng) {
    double total = 0;
    double nbAlea;
    double cumul = 0;
    int i;

    for (double p : poids) {
      total += p;
    }

    nbAlea = rng.nextDouble() * total;

    for (i = 0; i < poids.length; i++) {
      cumul += poids[i];
      if (nbAlea < cumul) {
        return i;
      }
    }
    return poids.length - 1;
  }

  /**
   * Cette fonction renvoie le nombre de portées qu'une lapine peut avoir en un an.
   *
   * @param rng : Instanciation de Mersenne Twister
   * @return : Renvoie le nombre de portées qu'une lapine peut avoir
   */
  private static int nombrePortee(MersenneTwister rng) {
    int[] possibilite = {3, 4, 5, 6, 7, 8, 9};
    double[] poids = {0.05, 0.1, 0.2, 0.3, 0.2, 0.1, 0.05};
    return possibilite[tirageProbaSelonPoids(poids, rng)];
  }

  /**
   * Cette fonction renvoie le nombre d'enfants qu'une lapine peut avoir à chaque portée.
   *
   * @param rng : Instanciation de Mersenne Twister
   * @return : renvoie le nombre d'enfants qu'une lapine peut avoir
   */
  private static int nombreEnfants(MersenneTwister rng) {
    int[] possibilite = {3, 4, 5, 6};
    double[] poids = {0.25, 0.25, 0.25, 0.25};
    return possibilite[tirageProbaSelonPoids(poids, rng)];
  }

  /**
   * Initialise la population de lapin composé de nbPaire de couple de lapin mature.
   *
   * @param nbPaire : Entier qui symbolise le nombre de couples de lapin
   * @param population : Instanciation d'une liste chainée servant à stocker les lapins
   * @param rng : Instanciation de Mersenne Twister
   * @return : renvoie la liste chainée modifiée avec les couples de lapins
   */
  public LinkedList<Lapin> popInit(int nbPaire, LinkedList<Lapin> population, MersenneTwister rng) {
    population.clear();
    nbBebeMort = 0;
    nbEnfantMort = 0;
    nbAdulteMort = 0;
    nbBebeMortTot = 0;
    nbEnfantMortTot = 0;
    nbAdulteMortTot = 0;
    nbLapinTot = 0;
    nbLapinMort = 0;
    nbLapinMortTot = 0;
    nbLapin = 0;
    for (int i = 0; i < nbPaire; i++) {
      population.add(new Lapin(6, 0, 5));
      population.add(new Lapin(6, 1, 5));
      nbLapinTot += 2;
      population.get(2 * i + 1).nbPorteeRestante = nombrePortee(rng);
    }

    return population;
  }

  /**
   * Simule des probabilités de mort de lapin plus domestiques.
   *
   * @param lapin : Instanciation de lapin
   * @param nbAlea : Nombre aléatoire qui sert à déterminer si le lapin en argument est mort
   * @return : True si le lapin est mort a la fin du mois ou false sinon
   */
  private boolean estMortV1(Lapin lapin, double nbAlea) {
    int a = Math.min(lapin.age, MAX_AGE);

    final double h = lapin.estMature ? hazardMatureV1[a] : hazardNonMatureV1[a];

    boolean mort = (nbAlea < h);

    if (!mort && lapin.estMature && lapin.age >= MAX_AGE) {
      mort = true;
    }

    if (mort) {
      if (lapin.estMature) {
        nbAdulteMort++;
        nbAdulteMortTot++;
      } else {
        nbEnfantMort++;
        nbEnfantMortTot++;
      }
    }

    return mort;
  }

  /**
   * Simule des probabilités de mort de lapin plus sauvage.
   *
   * @param lapin : Instanciation de lapin
   * @param nbAlea : Nombre aléatoire qui sert à determiner si le lapin en argument est mort
   * @return : True si le lapin est mort a la fin du mois ou false sinon
   */
  private boolean estMortV2(Lapin lapin, double nbAlea) {
    int a = Math.min(lapin.age, MAX_AGE);
    final double h = lapin.estMature ? hazardMatureV2[a] : hazardNonMatureV2[a];
    boolean mort = (nbAlea < h);
    if (!mort && lapin.estMature && lapin.age >= MAX_AGE) {
      mort = true;
    }
    if (mort) {
      if (lapin.estMature) {
        nbAdulteMort++;
        nbAdulteMortTot++;
      } else if (lapin.age < 1) {
        nbBebeMort++;
        nbBebeMortTot++;
      } else {
        nbEnfantMort++;
        nbEnfantMortTot++;
      }
    }
    return mort;
  }

  /**
   * Retire les lapins de la liste s'ils sont morts.
   *
   * @param population : Liste chainée qui contient les lapins
   * @param rng : Instanciation de Mersenne Twister
   * @return : renvoie la liste sans les lapins morts
   */
  public LinkedList<Lapin> verifMort(LinkedList<Lapin> population, MersenneTwister rng) {
    // System.out.println("test entre mort : " +l.get(3).age);
    LinkedList<Lapin> survivants = new LinkedList<>();
    for (Lapin lapin : population) {
      if (!estMortV2(lapin, rng.nextDouble())) {
        survivants.add(lapin);
      } else {
        nbLapinMort++;
        nbLapinMortTot++;
      }
    }
    // System.out.println("taille liste l : "+l.size());
    return survivants;
  }

  /**
   * Met à jour la liste en verifiant le nombre de portées par ans, la maturité des nouveaux nés et l'âge des lapins en mois.
   *
   * @param rng : Instanciation de Mersenne Twister
   * @param population : Instanciation d'une liste chainée pour stocker les lapins
   */
  private void miseAjourListe(MersenneTwister rng, LinkedList<Lapin> population) {
    for (Lapin lapin : population) {
      if (lapin.sexe == 1 && lapin.age % 12 == 0) {
        lapin.nbPorteeRestante = nombrePortee(rng);
      }

      lapin.age += 1;

      if (!lapin.estMature) {
        if (lapin.age >= 8) {
          lapin.estMature = true;
        } else if (lapin.age >= lapin.ageMaturite) {
          lapin.estMature = true;
        }
      }
    }
  }

  /**
   * Fait naitre les nouveaux lapins selon le nombre de portées et le nombre de lapins par portées.
   *
   * @param population : Instanciation d'une liste chainée pour stocker les lapins
   * @param rng : Instanciation  de Mersenne Twister
   * @return : Renvoie la liste chainée en argument avec les nouveaux lapins
   */
  public LinkedList<Lapin> naissance(LinkedList<Lapin> population, MersenneTwister rng) {
    boolean malePresent = false;
    Lapin currentLapin;

    Iterator<Lapin> it = population.iterator();
    while (it.hasNext() && !malePresent) {
      currentLapin = it.next();
      if (currentLapin.sexe == 0 && currentLapin.estMature) {
        malePresent = true;
      }
    }
    miseAjourListe(rng, population);

    if (!malePresent) {
      return population;
    }

    double[] poids = {0.25, 0.30, 0.45};
    int[] possibilite = {5, 6, 7};


    LinkedList<Lapin> nouveaux = new LinkedList<>();
    for (Lapin lapin : population) {

      if (lapin.sexe == 1 && lapin.estMature && lapin.nbPorteeRestante > 0) {
        if (lapin.nbEnfantEnGestation == 0) {
          lapin.nbPorteeRestante -= 1;
          lapin.nbEnfantEnGestation = nombreEnfants(rng);
        } else {
          for (int i = 0; i < lapin.nbEnfantEnGestation; i++) {
            // sexe aléatoire 0/1, ageMaturite tiré une fois à la naissance
            int sexe = rng.nextInt(2);
            int ageMaturite = possibilite[tirageProbaSelonPoids(poids, rng)];
            Lapin bebe = new Lapin(0, sexe, ageMaturite);
            nouveaux.add(bebe);
            nbLapinTot++;
            nbLapin++;
          }
          lapin.nbEnfantEnGestation = 0;
        }
      }
    }
    nouveaux.addAll(population);
    return nouveaux;
  }

  /**
   * Affiche le nombre de lapins actuels.
   *
   * @param nbMois : Numero du mois simulé
   * @param population : Instanciation d'une liste chainée qui contient les lapins
   */
  public void nombreActuelLapin(int nbMois, LinkedList<Lapin> population) {
    System.out.println("Nombre de lapins dans la population au mois " + nbMois + " : " + population.size());
    System.out.println("Pour " + nbLapinMortTot + " lapins morts et " + nbLapinTot + " naissances de lapins.");
    System.out.println("Pour un total de " + nbAdulteMortTot + " lapins matures morts, " + nbEnfantMortTot + " enfants morts et " + nbBebeMortTot + " lapins juvéniles morts au total.");
    System.out.println("Pour " + nbLapinMort + " lapins morts et " + nbLapin + " naissances de lapins.");
    System.out.println("Dont " + nbAdulteMort + " lapins matures, " + nbEnfantMort + " lapins non matures et " + nbBebeMort + " lapins juvéniles morts ce mois-ci\n");
  }

  /**
   * Simule plusieurs mois.
   *
   * @param nbMois : Durée de la simulation
   * @param population : Instanciation d'une liste chainée pour stocker les lapins
   * @param rng : instanciation de Mersenne Twister
   * @return : Renvoie la liste chainée après le fin de la simulation
   */
  public LinkedList<Lapin> simulation(int nbMois, LinkedList<Lapin> population, MersenneTwister rng, int numeroSimu) {
    int i;
    LoggerCsv logger = new LoggerCsv("logs/SimulationLapin", nbMois, numeroSimu);
    for (i = 0; i < nbMois; i++) {
      nbBebeMort = 0;
      nbEnfantMort = 0;
      nbAdulteMort = 0;
      nbLapinMort = 0;
      nbLapin = 0;
      population = simuler1Mois(population, rng);
      nombreActuelLapin(i + 1, population);

      logger.logMois(i + 1, nbLapinTot - nbLapinMortTot, nbLapinMortTot, nbBebeMortTot, nbEnfantMortTot, nbAdulteMortTot, nbLapin, nbBebeMort, nbEnfantMort, nbAdulteMort, nbLapinMort);

    }
    logger.close();
    return population;
  }

  /**
   * Simule in mois.
   *
   * @param population : Instanciation d'une liste chainée qui sert à stocker les lapins
   * @param rng : Instanciation de Mersenne Twister
   * @return : Renvoie la liste chainée modifiée après 1 mois de simulation
   */
  public LinkedList<Lapin> simuler1Mois(LinkedList<Lapin> population, MersenneTwister rng) {
    population = naissance(population, rng);
    population = verifMort(population, rng);
    return population;
  }
}

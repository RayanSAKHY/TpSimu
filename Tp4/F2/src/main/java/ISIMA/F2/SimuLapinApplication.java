package ISIMA.F2;

import ISIMA.F2.assets.Lapin;
import ISIMA.F2.assets.SimuUtils;
import java.util.LinkedList;
import org.apache.commons.math3.random.MersenneTwister;

/**
 * C'est la classe qui lance toute l'application.
 */
public class SimuLapinApplication {
  /**
   * Methode main qui utilise toutes les méthodes des clases auxiliaires.
   *
   * @param argv :
   */
  public static void main(String[] argv) {
    /*
      Initialisation de Mersenne Twister avec une seed donné
     */
    MersenneTwister rng = new MersenneTwister(887762873);
    /*
      Création de la liste chainée utilisée pour stocker les lapins
     */
    LinkedList<Lapin> population = new LinkedList<>();

    /*
    Initialisation de l'objet pour utiliser les fonctions de la simulation
     */
    SimuUtils s;
    /*
    Initialisation du temps de la simulation en mois
     */
    int tempsSimu = 204;
    int nbSimu = 1;
    int i;

    for (i = 0; i < nbSimu; i++) {
      s = new SimuUtils();
      /*
      Initialisation de la population avec 2 couples
       */
      population = s.popInit(2, population, rng);

      /*
      Calcul de la population après le temps de simulation voulu
       */
      population = s.simulation(tempsSimu, population, rng, i);
    }

  }
}

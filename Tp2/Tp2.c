#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "MerceneTwisterUnzip/mt19937ar-cok.c"

int *discreteDistribution(int nbClasse, int nbEssai)
{
    double seuilA = 0.5, seuilB = 0.65;
    int i = 0;
    double alea = 0.0;

    int *array = calloc(nbClasse, sizeof(int));
    unsigned long init[4] = {0x127, 0xE54, 0x94A5, 0x4F6}, length = 4;
    init_by_array(init, length);

    for (i = 0; i < nbEssai; i++)
    {
        alea = genrand_real1();
        // printf("alea : %f\n",alea);
        if (alea <= seuilA)
        {
            // printf("cas 1");
            array[0] += 1;
        }
        else if (alea <= seuilB)
        {
            array[1] += 1;
        }
        else
        {
            // printf("cas 3\n");
            array[2] += 1;
        }
    }
    return array;
}

double *CDF(int nbClasse, int *array)
{
    double *CDF = calloc(nbClasse, sizeof(double));
    int i = 0;
    int nbTirage = 0;
    int qtePrev = 0, qteActuel = 0;

    for (i = 0; i < nbClasse; i++)
    {
        nbTirage += array[i];
    }

    for (i = 0; i < nbClasse; i++)
    {
        qteActuel = array[i];
        // printf("qteprev: %d, qteact: %d,nbTirage: %d\n",qtePrev,qteActuel,nbTirage);
        CDF[i] = (double)(qtePrev + qteActuel) / nbTirage;
        qtePrev += qteActuel;
        // printf("CDF de i : %f\n",CDF[i]);
    }
    // printf("adr CDF: %x\n", (unsigned int) CDF);
    return CDF;
}

double negExp(double Mean)
{
    return -Mean * log(1 - genrand_real1());
}

void testNegExp(double Mean, int nbEssai)
{
    int total = 0;
    double *resultat = calloc(nbEssai, sizeof(double));
    int i = 0;

    for (i = 0; i < nbEssai; i++)
    {
        resultat[i] = negExp(Mean);
        total += resultat[i];
    }
    printf("Moyenne trouvé : %.2f\n", (double)total / nbEssai);
    free(resultat);
}

int main()
{
    int nbEssai = 1000, nbClasse = 3;
    int i = 0;
    int *tab3Classes;

    tab3Classes = discreteDistribution(nbClasse, nbEssai);

    for (i = 0; i < nbClasse; i++)
    {
        printf("Proba classe %d : %.2f\n", i + 1, (double)tab3Classes[i] / nbEssai * 100);
    }
    double *tabRes = CDF(nbClasse, tab3Classes);

    // printf("adr tabRes: %x\n", (unsigned int) tabRes);
    for (i = 0; i < nbClasse; i++)
    {
        printf("Proba cumulé classe %d : %.2f\n", i + 1, tabRes[i] * 100);
    }

    testNegExp(11, nbEssai);
    free(tab3Classes);
    free(tabRes);
    return 0;
}
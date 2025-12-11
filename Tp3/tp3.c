#include "../Tp2/MerceneTwisterUnzip/mt19937ar-cok.c"
#include <stdlib.h>
#include <stdio.h>
#include <math.h>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

double t_values[] = {
    12.706, // n = 1
    4.303,  // n = 2
    3.182,  // n = 3
    2.776,  // n = 4
    2.571,  // n = 5
    2.447,  // n = 6
    2.365,  // n = 7
    2.308,  // n = 8
    2.262,  // n = 9
    2.228,  // n = 10
    2.201,  // n = 11
    2.179,  // n = 12
    2.160,  // n = 13
    2.145,  // n = 14
    2.131,  // n = 15
    2.120,  // n = 16
    2.110,  // n = 17
    2.101,  // n = 18
    2.093,  // n = 19
    2.086,  // n = 20
    2.080,  // n = 21
    2.074,  // n = 22
    2.069,  // n = 23
    2.064,  // n = 24
    2.060,  // n = 25
    2.056,  // n = 26
    2.052,  // n = 27
    2.048,  // n = 28
    2.045,  // n = 29
    2.042,  // n = 30
    2.021,  // n = 40
    2.000,  // n = 80
    1.980,  // n = 120
    1.960   // n = inf
};

double t_students(int nbExp)
{
    int k = 0;
    if (nbExp <= 30)
        k = nbExp;
    else if (nbExp >= 30 && nbExp < 40)
        k = 30;
    else if (nbExp >= 40 && nbExp < 80)
        k = 31;
    else if (nbExp >= 80 && nbExp < 120)
        k = 32;
    else
        k = 33;

    return t_values[k];
}
double simuPi(int nbPoints)
{
    double nbPtsIn = 0;
    int i = 0;
    double x = 0.0, y = 0.0;
    for (i = 0; i < nbPoints; i++)
    {
        x = genrand_real1();
        y = genrand_real1();
        if (x * x + y * y < 1.)
            nbPtsIn++;
    }
    return 4. * nbPtsIn / (double)nbPoints;
}

double EstimateurVariance(int nbExp, int nbPoints)
{
    double *listeVal = calloc(nbExp, sizeof(double));
    int i = 0;
    double sum = 0.0, mean = 0.0;
    for (i = 0; i < nbExp; i++)
    {
        listeVal[i] = simuPi(nbPoints);
    }
    for (i = 0; i < nbExp; i++)
    {
        sum += listeVal[i];
    }
    mean = sum / (double)nbExp;
    printf("Moyenne pi : %1.5lf\n", mean);
    sum = 0.0;
    for (i = 0; i < nbExp; i++)
    {
        printf("val : %1.5lf   ", listeVal[i]);
        sum += (listeVal[i] - mean) * (listeVal[i] - mean);
        printf("sum : %1.7lf\n", sum);
    }
    free(listeVal);
    return sum / (double)(nbPoints - 1);
}

double confidenceRadiusPi(int nbExp, int nbPoints)
{
    double variance = EstimateurVariance(nbExp, nbPoints);
    printf("Estimateur variance de pi : %1.5lf\n", variance);
    double R = t_students(nbExp) * sqrt(variance / (double)nbPoints);
}

int main()
{
    unsigned long init[4] = {0x123, 0x234, 0x345, 0x456}, length = 4;
    init_by_array(init, length);
    double confidenceRadius = 0.0;
    confidenceRadius = confidenceRadiusPi(5, 10000000);
    printf("Rayon de confiance de pi : %1.5lf\n", confidenceRadius);
    return 0;
}
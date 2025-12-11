#include <stdio.h>
#include <stdlib.h>



long MiddleSquareTechnique(long seed,int degree){
    int i=0;
    long number=seed;
    for (i=0;i<degree;i++){
        printf("N%d : %ld\n",i,number);
        number=seed*seed;
        number=number/100%10000;
        seed=number;
    }
    return number;
}

int CoinTossing(int try){
    int NumberTrue=0;
    int NumberFalse=0;
    int alea=rand()%2;
    int i=0;
    for (i=0;i<try;i++){
        if (alea){
            NumberTrue+=1;
        }
        else{
            NumberFalse+=1;
        }
        alea=rand()%2;
    }
    printf("Nombre d'essai : %d\n",try);
    printf("Nombre de Pile : %d   ",NumberTrue);
    printf("Nombre de Face : %d\n",NumberFalse);
    return 0;
}

int De(int try,int NbFace){
    int* faces=calloc(NbFace,sizeof(int));
    int i=0;
    int alea=0;
    for (i=0;i<try;i++){
        alea=rand()%NbFace;
        faces[alea]++;
    }
    printf("Nombre d'essai : %d\n",try);
    for (i=0;i<NbFace;i++){
        printf("Nombre de %d : %d   ",i+1,faces[i]);
    }
    printf("\n");
    free(faces);
    return 0;
}

int intRand(int seed,int a,int c,int m,int try){
    int i=0;
    for (i=0;i<try;i++){
        seed=(a*seed+c)%m;
        printf("%d, ",seed);
    }
    printf("\n");
    return seed;
}

int floatRand(int seed,int a,int c,int m,int try){
    int i=0;
    for (i=0;i<try;i++){
        seed=(a*seed+c)%m;
        printf("%0.4f, ",(float) seed/16.0);
    }
    printf("\n");
    return (float) seed/16.0;
}

int shiftRegister4Bits(int degree,int seed){
    int premierBit=0;
    int secondBit=0;
    int bitxor =0;
    int i=0;

    for (i=0;i<degree;i++){
        premierBit = seed & 1;
        secondBit = (seed & 2);

        secondBit >>= 1;

        //printf(" debug bits 1: %04b  bit 2 : %04b\n",premierBit,secondBit);    

        bitxor = premierBit ^ secondBit;

        //printf(" xor bit: %04b\n",bitxor);

        seed >>= 1;

        //printf(" seed: %04b\n", seed);

        if (bitxor) seed |= 8;

        //printf("seed: %04b\n",seed);
        printf("%04b  ",seed);
    }
    printf("\n");
    return seed;
}

int main(){
    //MiddleSquareTechnique(3141,100);

    /*CoinTossing(10);
    CoinTossing(100);
    CoinTossing(1000);
    CoinTossing(1000000);*/

    /*int nbFace=10;
    De(10,nbFace);
    De(100,nbFace);
    De(1000,nbFace);
    De(1000000,nbFace);*/

    /*int a=5;
    int c=3;
    int x0=7;
    int m=23;
    intRand(x0,a,c,m,32);
    floatRand(x0,a,c,m,32);*/

    shiftRegister4Bits(4,0b0110);
    return 0;
}
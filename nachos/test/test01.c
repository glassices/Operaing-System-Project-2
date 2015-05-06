#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#define BUFSIZE 1024

char buf[BUFSIZE];
char buf2[BUFSIZE];
int main(){
    int readNumber=read(10,buf,2);
    int fda = creat("aa");

    close(fda);
    readNumber=read(fda,buf,2);
    fda=open("aa");
    unlink("aa");
    close(fda);
    return 0;
}
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#define BUFSIZE 1024

char buf[BUFSIZE];
char buf2[BUFSIZE];
int main(){
    int fda = creat("aa");
    int fdb = creat("bb");

    close(fda);
    close(fdb);
    fda=open("aa");
    unlink("aa");
    close(fda);
    unlink("bb");
    return 0;
}
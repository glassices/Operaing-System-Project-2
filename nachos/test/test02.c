#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#define BUFSIZE 1024

char buf[BUFSIZE];
char buf2[BUFSIZE];
int main(){
    int fda = creat("aa");
    int fdb = creat("bb");
    buf[0]='a';
    buf[1]='b';
    int wroteNumber=write(fda,buf,2);
    printf("wrote number: %d\n",wroteNumber);
    close(fda);
    close(fdb);
    fda=open("aa");
    unlink("aa");
    int readNumber=read(fda,buf2,2);
    printf("read number: %d\n",readNumber);
    printf("%d,%d\n",buf2[0],buf2[1]);
    close(fda);
    unlink("bb");
    return 0;
}
/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks FRI Folding Hardware Accelerator
 * Chisel hardware for binary FRI evaluation-table folding over the Goldilocks field.
 * Repository: https://github.com/OpenRTL-Lab/fri-fold-goldilocks
 * File: operators/fri-fold/cmodel/reference.c
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks FRI Fold contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <assert.h>

static const uint64_t P=UINT64_C(0xffffffff00000001);
static uint64_t add(uint64_t a,uint64_t b){return (uint64_t)(((__uint128_t)a+b)%P);}
static uint64_t sub(uint64_t a,uint64_t b){return a>=b?a-b:P-(b-a);}
static uint64_t mul(uint64_t a,uint64_t b){return (uint64_t)(((__uint128_t)a*b)%P);}
static uint64_t power(uint64_t x,uint64_t n){uint64_t r=1;while(n){if(n&1)r=mul(r,x);x=mul(x,x);n>>=1;}return r;}
static uint64_t seed=UINT64_C(0x13073d82391990a7);
static uint64_t random_field(void){seed^=seed<<13;seed^=seed>>7;seed^=seed<<17;return seed%P;}
static uint64_t eval(const uint64_t*c,int n,uint64_t x){uint64_t r=0;for(int i=n-1;i>=0;i--)r=add(mul(r,x),c[i]);return r;}
static uint64_t direct(uint64_t a,uint64_t b,uint64_t u,uint64_t alpha){
  const uint64_t inv2=UINT64_C(0x7fffffff80000001);
  return mul(add(add(a,b),mul(mul(alpha,u),sub(a,b))),inv2);
}
static FILE *file(const char*directory,const char*name){char path[4096];assert(snprintf(path,sizeof(path),"%s/%s",directory,name)>0);FILE*f=fopen(path,"wx");assert(f);return f;}
int main(int argc,char**argv){
  assert(argc==4);unsigned m=(unsigned)strtoul(argv[2],0,10),tasks=(unsigned)strtoul(argv[3],0,10);
  assert(m>=8&&m<=32768&&(m&(m-1))==0&&tasks>=16);
  uint64_t kat[]={3,5,7,11};uint64_t folded[]={add(3,mul(13,5)),add(7,mul(13,11))};
  assert(eval(folded,2,4)==668&&direct(eval(kat,4,2),eval(kat,4,P-2),power(2,P-2),13)==668);
  FILE*input=file(argv[1],"input.hex"),*expected=file(argv[1],"expected.hex"),*alphaFile=file(argv[1],"alpha.hex"),*meta=file(argv[1],"polynomials.txt");
  unsigned logn=0;for(unsigned n=m*2;n>1;n>>=1)logn++;
  uint64_t root=power(7,(P-1)>>(logn));
  assert(power(root,m)==P-1&&power(root,2*m)==1);
  for(unsigned t=0;t<tasks;t++){
    uint64_t alpha=random_field(),coeff[8],fold[4];for(int k=0;k<8;k++)coeff[k]=random_field();
    unsigned kind=t%16;
    if(kind==0){alpha=13;for(int k=0;k<8;k++)coeff[k]=k<4?kat[k]:0;}
    if(kind==1){for(int k=1;k<8;k++)coeff[k]=0;}
    if(kind==2){for(int k=0;k<8;k+=2)coeff[k]=0;}
    if(kind==3){for(int k=1;k<8;k+=2)coeff[k]=0;}
    if(kind==4)alpha=0;
    if(kind==5)alpha=1;
    if(kind==6)alpha=P-1;
    fprintf(alphaFile,"%016" PRIx64 "\n",alpha);
    fprintf(meta,"%u %u %016" PRIx64,t,kind,alpha);for(int k=0;k<8;k++)fprintf(meta," %016" PRIx64,coeff[k]);fprintf(meta,"\n");
    for(int k=0;k<4;k++)fold[k]=add(coeff[2*k],mul(alpha,coeff[2*k+1]));
    uint64_t x=kind==0?2:7,u=power(x,P-2),invRoot=power(root,P-2);
    for(unsigned i=0;i<m;i++){
      uint64_t a=eval(coeff,8,x),b=eval(coeff,8,P-x),g=eval(fold,4,mul(x,x));
      if(kind>=8){a=random_field();b=random_field();g=direct(a,b,u,alpha);}
      if(kind==8&&i<4){a=i&1?P-1:0;b=i&2?P-1:0;g=direct(a,b,u,alpha);}
      assert(mul(x,u)==1&&direct(a,b,u,alpha)==g);
      fprintf(input,"%016" PRIx64 "\n%016" PRIx64 "\n%016" PRIx64 "\n",a,b,u);
      fprintf(expected,"%016" PRIx64 "\n",g);x=mul(x,root);u=mul(u,invRoot);
    }
  }
  fclose(input);fclose(expected);fclose(alphaFile);fclose(meta);
  printf("C_REFERENCE pass=1 tasks=%u pairs=%u checked=%u kat=668\n",tasks,m,tasks*m);return 0;
}

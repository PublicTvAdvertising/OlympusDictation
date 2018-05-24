/*
  NAME: icsiargs.c

  DESCRIPTION:
     process arguments in new ICSI format.  supplies function "icsiargs",
     which is given argc, argv, and an argument table
  NOTES:

  REVISION HISTORY:
      jsegal     06/01/94 - Creation

 * $Log: icsiargs.c  $
 * Revision 1.5 2003/10/20 16:27:14CEST kroener 
 * 
 * Revision 1.4 2003/10/16 16:02:23CEST kroener 
 * 
 * Revision 1.3 2003/10/14 16:56:04CEST kroener 
 * 
 * Revision 1.2 2003/10/10 16:34:22CEST kroener 
 * 
 * Revision 1.1 2003/07/29 09:55:31CEST kroener 
 * Initial revision
 * Member added to project e:/Projekt/GBS/DSS/ABS/ABS.pj
 * Revision 1.3  1994/12/10  02:45:58  davidj
 * Removed const on apname.
 *
 * Revision 1.2  1994/12/10  00:14:27  davidj
 * Updates.
 *
 * Revision 1.1  1994/12/09  22:35:50  davidj
 * Initial revision
 *
 * Revision 1.6  1994/09/28  23:21:36  jsegal
 * Fix little error bug
 *
 * Revision 1.5  1994/06/08  02:35:14  jsegal
 * add printargs
 *
 * Revision 1.4  1994/06/08  02:05:27  jsegal
 * minor cosmetic changes
 *
 * Revision 1.3  1994/06/08  01:41:26  jsegal
 * Automatically print default value for optional arguments
 *
 * Revision 1.2  1994/06/03  02:26:24  jsegal
 * fix headers a bit
 *
 * Revision 1.1  1994/06/03  02:17:23  jsegal
 * enter under source control
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "qp_ipkclib.h"


#include <string.h>
namespace QualityPlay {

#define INT_ERROR {show_usage(aname,tab); return(-1);}


static icsiargtab_t *get_arg_el(const char *argname, icsiargtab_t *argtab );
static void show_usage( const char *aname, const icsiargtab_t *argtab );
static int get_num_args( const icsiargtab_t *argtab );
static int get_bool_val( const char *value );

static const char *typenames[] =
{
  "",
  "",
  "int",
  "long",
  "string",
  "boolean",
  "float",
  "double"
};



/*---------------------------------icsiargs----------------------------------*/
/*
  icsiargs is the main function you call to actually parse the arguments as
  given.  To use this function, you must first set up an argument
  descriptor table.  This table is a NULL-terminated array of elements of
  icsiargtab_t.  Each icsiargtab_t is a structure containing the following
  elements:
  char *argname;       the name of the argument, a string
  char *argdesc;       a textual description, of the arg, displayed as help
  icsiarg_t argtype;   the type of the argument, see below
  void *varptr;        a pointer to the variable which will contain the
                       specified value
  icsiargr_t argreq;   specifies if the argument is required or optional
                       (the default is for an argument to be optional)

  argtype can be one of
  ARG_INT, ARG_LONG, ARG_STR, ARG_BOOL, ARG_FLOAT, ARG_DOUBLE, or
  ARG_DESC.  ARG_DESC entries are dummy entries which you can
  use to supply more documentation in the usage message.  The current
  paradigm for ARG_BOOL is that the variable must be a C int, and if the
  value of a variable is specified as "1", "[tT].*" or "[yY].*", the
  variable will be set to TRUE, otherwise it will be set to false.
  for ARG_STR, the variable will be set to a malloced copy of the string
  specified on the command-line.
  argreq is one of ARG_REQ or ARG_OPT (note:  if you leave this out of a
  static initializer for the structure, ARG_OPT will be the default).  If it
  is ARG_REQ the argument will be required to be specified, if it is
  ARG_OPT, the argument is optional.
  
  icsiargs will parse the command-line, and will set the variables named in
  the argument table to the values specified on the command-line.  If
  appname is non-null, it will set the variable pointed to by appname to be
  a pointer to a malloced copy of the name which the application was called
  by.  It will modify argc and argv, eliminating the references to the
  arguments with "=" in them.  The remaining argc positional arguments will
  be left in the variable argv.  Note:  if somebody types a command of the
  form
  % command arg1=5 foo arg2=7
  icsiargs will set the variable referred to by arg1 to 5, and will set argc
  to 2 and argv to the array {"foo","arg2=7",NULL}

  Author:  Jonathan Segal  jsegal@icsi.berkeley.edu
  */

int icsiargs(icsiargtab_t *tab, int *argc, const char ***argv, char **appname)
{
  const char   *argname;
  char         *value;
  icsiargtab_t *ael;
  char         *cp;
  const char   *aname;
  int           numargs;                         /* number of args in argtab */
  char         *reqs;
  int           flag;
  int           numset = 0;
  
  aname = (*argv)[0];
  if (appname) *appname = (char *) aname; /* set the app name */

  numargs = get_num_args(tab);
  reqs = (char *) calloc(numargs, sizeof(char));
  
  /* skip args past app name, and get the args */
  while ((*argv)++,--*argc)
  {
    
    /* index replaced by strchr (A. Kipp 02.02.96) */
        value = strchr((char*)**argv,'=');   /* find the '=' */
    if (!value) 
    {
      /* no '=' found; try to get a ARG_BOOL */
    }
    else
    {
       *value = '\0';
       ++value;
    }
    argname = **argv;
    if (!(ael = get_arg_el(argname, tab)) || !strcmp(argname,"help") || !strcmp(argname,"?"))
    {
      if (strcmp(argname,"help") && strcmp(argname,"?")) fprintf(stderr,"Unkown argument \"%s\"\n",argname);
      INT_ERROR
    }
    if (reqs[ael - tab]) 
    {
      fprintf(stderr,"Error: argument %s multiply set!\n",argname);
      INT_ERROR
    }
    reqs[ael-tab] = 1;
    ++numset;
    switch (ael->argtype) 
    {
    case ARG_INT:
      if(value)
      {
         *((int *) ael->varptr) = (int) strtol(value,&cp,10);
         if (cp == value) 
         {
            fprintf(stderr,"Warning: arg %s should be an int!\n",argname);
            INT_ERROR
         }
      }
      break;
    case ARG_LONG:
      if(value)
      {
         *((long int *) ael->varptr) = strtol(value,&cp,10);
         if (cp == value) 
         {
            fprintf(stderr,"Warning: arg %s should be a long int!\n",argname);
            INT_ERROR
         }
      }
      break;
    case ARG_STR:
      if(value) *((char **) ael->varptr) = (char *) strdup(value);
      break;
    case ARG_BOOL:
      *((int *) ael->varptr) = get_bool_val(value);
      break;
    case ARG_FLOAT:
      if(value) 
      {
         if (!sscanf(value,"%g",((float *) ael->varptr)))
         {
            fprintf(stderr,"Warning: arg %s should be a float!\n",argname);
            INT_ERROR
         }
      }
      break;
    case ARG_DOUBLE:
      if(value) 
      {
         if (!sscanf(value,"%lg",((double *) ael->varptr)))
         {
            fprintf(stderr,"Warning: arg %s should be a double!\n",argname);
            INT_ERROR
         } 
      }
      break;
    default:
      fprintf(stderr,"Internal error!! unknown arg type %d for arg %s!\n",ael->argtype,argname);
      fprintf(stderr," This should not happen!! contact %s maintainer!\n",aname);
      return(-1);
      break;
    }
  }
  /* now make sure all the required args are set */

  flag = 0;
  for (ael = tab, cp = reqs; numargs; ++ael, ++cp, --numargs) 
  {
    if (ael->argreq && !*cp) 
    {
      fprintf(stderr,"  Argument \"%s\" is required, but not set!\n", ael->argname);
      flag = 1;
    }
  }
  if (flag) INT_ERROR
  free(reqs);
  return numset;
}


/*--------------------------------get_arg_el---------------------------------*/
/*
  given an argument name and an argument table, return the argument table
  entry which ocrresponds to it.
*/

static icsiargtab_t* get_arg_el( const char *argname, icsiargtab_t *argtab )
{
  for (; argtab->argtype != ARG_NOMOREARGS; ++argtab)
/* --- changed by Michael Kroener 2003.06.24 to support parameter without regard to their case --- */
    if (argtab->argname && (!strcmp(argtab->argname, argname)) ) return argtab;
    /* if (argtab->argname && (!strcmp(argtab->argname, argname)) ) return argtab;*/
  return (icsiargtab_t *) 0;
}

/*--------------------------------show_usage---------------------------------*/
/*
Enter function description here
*/

static void show_usage( const char *aname, const icsiargtab_t *argtab )
{
  const char *typen;
  FILE *fp;
  fp=stderr;

  /*fprintf(fp,"\n%s:\n",aname);*/
  fprintf(fp,"\n  ======================================================================\n");
  for(; argtab->argtype != ARG_NOMOREARGS; ++argtab)
  {
    typen = typenames[argtab->argtype];
    if (argtab->argtype == ARG_DESC) 
    {
      fprintf(fp,"  %s\n\n",argtab->argdesc);
      fprintf(fp,"  %-15s%-36s%-9s  %-8s\n","parameter", "description", "  type", " value");
      fprintf(fp,"  ======================================================================\n");
    }
    else
    {
      
      fprintf(fp,"  %-15s%-36s %s%7s%s%s",argtab->argname,argtab->argdesc,*typen?"(":"", typen, *typen?")":"",argtab->argreq?" ( *REQ*)":"");
      /* print default value.  Note only non-required options have default */
      /* values*/
      if (!argtab->argreq) 
      {
        switch (argtab->argtype) 
        {
        case ARG_INT:
          fprintf(fp," (%6d)", *((int *)argtab->varptr));
          break;
        case ARG_LONG:
          fprintf(fp," (%6ld)", *((long int *) argtab->varptr));
          break;
        case ARG_STR:
          fprintf(fp," (%6s)", *((char **) argtab->varptr)?*((char **)argtab->varptr):"null");
          break;
        case ARG_BOOL:
          fprintf(fp," (%6s)", *((int *) argtab->varptr)?"true":"false");
          break;
        case ARG_FLOAT:
          fprintf(fp," (%6g)", *((float *) argtab->varptr));
          break;
        case ARG_DOUBLE:
          fprintf(fp," (%6g)", *((double *) argtab->varptr));
          break;
        default:
          fprintf(fp,"Internal error!! unknown arg type %d for arg %s!\n",argtab->argtype,argtab->argname);
          fprintf(fp," This should not happen!! contact %s maintainer!\n",aname);
          exit(1);
          break;
        }
      }
      fprintf(fp,"\n");
    }
  }
}

/*-------------------------------get_bool_val--------------------------------*/
/*
  return TRUE if the string pointed to by value is a "true" string, false
  otherwise.  In this implementation, if the string is "1", or begins with a
  "t" or a "y" (any case), it is true, otherwise it is false
  the NULL or empty string are each false, obviously
  */

static int get_bool_val( const char *value )
{
  char c;
  if (!value) 
  {
    return(!value); /* just the argument name ist given, but no value => return(true); */ 
  }
  else
  {
    c = *value;
    return (c == '1' || c == 't' || c == 'T' || c == 'y' || c == 'Y');
  }
  return(0);
}


/*-------------------------------get_num_args--------------------------------*/
/*
  count the number of elements in the argtab
*/

static int get_num_args( const icsiargtab_t *argtab )
{
  int i = 0;
  for(i = 0; argtab->argtype != ARG_NOMOREARGS; ++argtab) ++i;
  return (i);
}


/*---------------------------------printargs---------------------------------*/
/*
  This function will print all the arguments and values as set to a given
  file (STDERR if fp is NULL).  This function will be useful for diagnostic
  output, e.g. to log files.   If appname is not null, it will display that,
  too.
*/

void printargs( FILE *fp, const char *appname, const icsiargtab_t *argtab )
{
  int i = 0;
  if (fp == 0) fp = stderr;
  fprintf(fp,"\n%s \n\nwas invoked with the following argument values\n(some of which may be defaults):\n\n",appname?appname:"This");
  for(i = 0; argtab->argtype != ARG_NOMOREARGS; ++argtab)
  {
    if (argtab->argtype == ARG_DESC)
      continue;
    fprintf(fp,"  %-15s= ",argtab->argname);
    switch (argtab->argtype) 
    {
    case ARG_INT:
      fprintf(fp,"%-8d", *((int *)argtab->varptr));
      break;
    case ARG_LONG:
      fprintf(fp,"%-8ld", *((long int *) argtab->varptr));
      break;
    case ARG_STR:
      fprintf(fp,"%-8s", *((char **) argtab->varptr)?*((char **)argtab->varptr):"(null)");
      break;
    case ARG_BOOL:
      fprintf(fp,"%-8s", *((int *) argtab->varptr)?"true":"false");
      break;
    case ARG_FLOAT:
      fprintf(fp,"%-8g", *((float *) argtab->varptr));
      break;
    case ARG_DOUBLE:
      fprintf(fp,"%-8g", *((double *) argtab->varptr));
      break;
    default:
      fprintf(fp,"Internal error!! unknown arg type %d for arg %s!\n",argtab->argtype,argtab->argname);
      fprintf(fp," This should not happen!! contact %s maintainer!\n",appname?appname:"its");
      exit(1);
      break;
    }
    fprintf(fp,"\n");
  }
  fprintf(fp,"\n");
}


}

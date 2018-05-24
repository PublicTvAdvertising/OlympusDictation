#ifndef FILE
# include <stdio.h>
#endif 

#ifndef IPKCLIB

#define IPKCLIB
/* LABEL								*/
# define LABANZ		100	/* max. anzahl labeln pro satz	*/

namespace QualityPlay {

/*######################################################################*/

/*
  icsiarg_t denotes the type of the variable which corresponds to the
  argument.
  */
typedef enum icsiarg_e 
{
  ARG_NOMOREARGS,
  ARG_DESC,
  ARG_INT,
  ARG_LONG,
  ARG_STR,
  ARG_BOOL,
  ARG_FLOAT,
  ARG_DOUBLE,
  ARG_NUMARGTYPES,
} icsiarg_t;

typedef enum icsiargr_e
{
   ARG_OPT,                                          /* argument is optional */
   ARG_REQ                                           /* argument is required */
} icsiargr_t;


typedef struct icsiargtab_s
{
  const char * argname;                              /* name of the argument */
  const char * argdesc;      /* text description of the arg (for usage info) */
  icsiarg_t    argtype;                              /* type of the argument */
  void        *varptr;            /* the variable which gets the arg's value */
  icsiargr_t   argreq;                          /* is the argument required? */
} icsiargtab_t;

/* #####################################################################*/



/* ICSIARGS		*/
int icsiargs( icsiargtab_t *tab, int *argc, const char ***argv, char **appname );
/*
  icsiargs is the main function you call to actually parse the arguments as
  given.  To use this function, you must first set up an argument
  descriptor table tab.  This table is a NULL-terminated array of elements of
  icsiargtab_t.  Each icsiargtab_t is a structure containing the following
  elements:
  const char *argname; the name of the argument, a string
  const char *argdesc; a textual description, of the arg, displayed as help
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

  icsiargs will return the number of arguments it successfully set.
  
  Author:  Jonathan Segal  jsegal@icsi.berkeley.edu
  */
void printargs( FILE *fp, const char *appname, const icsiargtab_t *argtab );
/*
  This function will print all the arguments and values as set to a given
  file (STDERR if fp is NULL).  This function will be useful for diagnostic
  output, e.g. to log files.   If appname is not null, it will display that,
  too.
*/
}
#endif

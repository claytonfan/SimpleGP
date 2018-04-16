/* 
 * Program:   simple_gp.java
 *
 * Based on tiny_gp by 
 *   Riccardo Poli (email: rpoli@essex.ac.uk)
 *
 */
import java.util.*;
import java.io.*; 

public class simple_gp {
  //
  // Parameters
  //
  static final int  
    MAXLEN      = 10000,      // Max program length  
    POPSIZE     = 100000,     // Population
    DEPTH       = 5,          // Depth of tree
    MAXGENS     = 100,        // Max number of generations
    TSIZE       = 2;          // Number of tounament competitors
  static final double  
    PMUT_PER_NODE  = 0.05,    // Mutation rate
    CROSSOVER_PROB = 0.9;     // Crossover rate
  //
  // Evolution data
  //
  static double [] fitness;  // added static
  static double [][] targets;
  static char [][] pop;      // added static
  static Random rd = new Random();
  static int varnumber, fitnesscases, randomnumber;
  static double fbestpop = 0.0, favgpop = 0.0;
  static long seed;
  static double avg_len; 
  //
  // Program definition and data
  //
  static final int 
    ADD = 110, 
    SUB = 111, 
    MUL = 112, 
    DIV = 113,
    FSET_START = ADD, 
    FSET_END = DIV;
  static double [] constants = new double[FSET_START]; // random constants
  static double minrandom, maxrandom;                  // range for constants
  // Program string and its index
  static char [] prog;
  static int iprog;
  //
  // ********************************************************************
  //
  // Class for displaying program data
  //
  public static class Display {
    // Display program for an individual
    private static int program( char []buffer, int buffercounter ) {
      int a1=0, a2;
      if ( buffer[buffercounter] < FSET_START ) {   // ?????????
        if ( buffer[buffercounter] < varnumber )
          System.out.print( "X"+ (buffer[buffercounter] + 1 )+ " ");
        else
          System.out.print( constants[buffer[buffercounter]]);
        return( ++buffercounter );
      }
      switch(buffer[buffercounter]) {
        case ADD:
          System.out.print( "(");
          a1 = program( buffer, ++buffercounter ); 
          System.out.print( " + "); 
          break;
        case SUB:
          System.out.print( "(");
          a1 = program( buffer, ++buffercounter ); 
          System.out.print( " - "); 
          break;
        case MUL:
          System.out.print( "(");
          a1 = program( buffer, ++buffercounter ); 
          System.out.print( " * "); 
          break;
        case DIV:
          System.out.print( "(");
          a1 = program( buffer, ++buffercounter ); 
          System.out.print( " / "); 
          break;
      }
      a2 = program( buffer, a1 ); 
      System.out.print( ")"); 
      return( a2 );
    }
    //
    // -- Public --
    //
    // Print parameters
    public static void params() {
      System.out.print("-- Simple GP (Java version) --");
      System.out.print(
                       "\nSeed for random number generation = "+seed+
                       "\nMaximum program length            = "+MAXLEN+
                       "\nPopulation size                   = "+POPSIZE+
                       "\nMaximum depth of tree             = "+DEPTH+
                       "\nCrossover probability             = "+CROSSOVER_PROB+
                       "\nMutation probability              = "+PMUT_PER_NODE+
                       "\nMinimum random constant value     = "+minrandom+
                       "\nMaximum random constant value     = "+maxrandom+
                       "\nNumber of fitness cases           = "+fitnesscases+
                       "\nMaximum number of generations     = "+MAXGENS+
                       "\nNumber of tournament competitors  = "+TSIZE+
                       "\n--------------------------------------------------\n");
    }
    // Print statistics
    public static void stats( double [] fitness, char [][] pop, int gen ) {
      int best = rd.nextInt(POPSIZE);
      int node_count = 0;
      fbestpop = fitness[best];
      favgpop = 0.0;
      for( int i = 0; i < POPSIZE; i++ ) {
        node_count += Tree.traverse( pop[i], 0 );
        favgpop += fitness[i];
        if ( fitness[i] > fbestpop ) {
          best = i;
          fbestpop = fitness[i];
        }
      }
      avg_len = (double) node_count / POPSIZE;
      favgpop /= POPSIZE;
      System.out.print("***** Generation="+gen+" Avg Fitness="+(-favgpop)+
                       " Best Fitness="+(-fbestpop)+" Avg Size="+avg_len+
                       "\nBest Individual: ");
      Display.program( pop[best], 0 );
      System.out.print( "\n");
      System.out.flush();
    }                                
  }                                        // Class Display
  //
  // Class for population processing
  //
  public static class Population {
    // Buffer to hold a individual program
    private static char [] population = new char[MAXLEN];
    // Grow
    private static int grow( char [] buffer, int pos, int max, int depth ) {
      char prim = (char) rd.nextInt(2);
      int one_child;
      if ( pos >= max ) return( -1 );
      if ( pos == 0   ) prim = 1;
      if ( prim == 0 || depth == 0 ) {
        prim = (char) rd.nextInt( varnumber + randomnumber );
        buffer[pos] = prim;
        return(pos+1);
      }
      else  {
        prim = (char) (rd.nextInt(FSET_END - FSET_START + 1) + FSET_START);
        switch(prim) {
          case ADD: 
          case SUB: 
          case MUL: 
          case DIV:
            buffer[pos] = prim;
            one_child = grow( buffer, pos+1, max,depth-1 );
            if ( one_child < 0 ) return( -1 );
            return( grow( buffer, one_child, max,depth-1 ) );
        }
      }
      // Should never get here
      System.out.println( "ERROR: Program error at grow()." );
      return( 0 ); // should never get here
    }
    // Create program for one individual
    private static char [] create_indiv( int depth ) {
      char [] ind;
      int len;
      len = grow( population, 0, MAXLEN, depth );
      
      while( len < 0 )
        len = grow( population, 0, MAXLEN, depth );
      ind = new char[len];
      System.arraycopy( population, 0, ind, 0, len ); 
      return( ind );
    }
    // Interpret program -- calculate value
    private static double run() {
      char primitive = prog[iprog++];
      if ( primitive < FSET_START )
        return( constants[primitive] );
      switch ( primitive ) {
        case ADD : return( run() + run() );
        case SUB : return( run() - run() );
        case MUL : return( run() * run() );
        case DIV : { 
          double num = run(), den = run();
          if ( Math.abs( den ) <= 0.001 ) 
            return( num );
          else 
            return( num / den );
        }
      }
      // Should never get here
      System.out.println( "ERROR: Program error at run()." );
      return( 0.0 );
    }
    //
    // -- Public --
    //
    // Compute fitness of an individual program
    public static double fitness( char [] Prog ) {
      double result, fit = 0.0;
      int len = Tree.traverse( Prog, 0 );
      for( int i = 0; i < fitnesscases; i++ ) {
        for( int j = 0; j < varnumber; j++ ) {
          constants[j] = targets[i][j];
        }
        prog = Prog;
        iprog = 0;
        result = run();
        fit += Math.abs( result - targets[i][varnumber]);
      }
      return( -fit );
    }
    // Create population
    public static char [][] create( int n, int depth, double [] fitness ) {
      char [][]pop = new char[n][];
      for ( int i = 0; i < n; i++ ) {
        pop[i]     = create_indiv( depth );
        fitness[i] = fitness( pop[i] );
      }
      return( pop );
    } 
  }                           // Class Population
  //
  // Class for prog tree processing
  //
  public static class Tree {
    public static int traverse( char [] buffer, int buffercount ) {
      if ( buffer[buffercount] < FSET_START )
        return( ++buffercount );   
      switch(buffer[buffercount]) {
        case ADD: 
        case SUB: 
        case MUL: 
        case DIV:
          return( traverse( buffer, traverse( buffer, ++buffercount ) ) );
      }
      return( 0 ); // should never get here
    }
  }                                       // Class Tree
  //
  // Class for tournament
  //
  public static class Tournament {
   // Find most fit
    public static int positive( double [] fitness, int tsize ) {
      int best = rd.nextInt(POPSIZE), competitor;
      double fbest = -1.0e34;
      for ( int i = 0; i < tsize; i++ ) {
        competitor = rd.nextInt(POPSIZE);
        if ( fitness[competitor] > fbest ) {
          fbest = fitness[competitor];
          best = competitor;
        }
      }
      return( best );
    } 
    // Find least fit
    public static int negative( double [] fitness, int tsize ) {
      int worst = rd.nextInt(POPSIZE), competitor;
      double fworst = 1e34;
      for ( int i = 0; i < tsize; i++ ) {
        competitor = rd.nextInt(POPSIZE);
        if ( fitness[competitor] < fworst ) {
          fworst = fitness[competitor];
          worst = competitor;
        }
      }
      return( worst );
    }
  }                                   // Class tournament
  //
  // ********************************************************************
  //
  // Get values for fitness parameters and input data
  //
  void setup_gp(String fname) {
    try { 
      BufferedReader in      = new BufferedReader( new FileReader(fname) );
      String line            = in.readLine();
      StringTokenizer tokens = new StringTokenizer(line);
      varnumber    = Integer.parseInt(tokens.nextToken().trim());
      randomnumber = Integer.parseInt(tokens.nextToken().trim());
      minrandom    = Double.parseDouble(tokens.nextToken().trim());
      maxrandom    = Double.parseDouble(tokens.nextToken().trim());
      fitnesscases = Integer.parseInt(tokens.nextToken().trim());
      targets      = new double[fitnesscases][varnumber+1];
      if( varnumber + randomnumber >= FSET_START ) {
        System.out.println("WARNING: Too many variables and constants");
      }
      for (int i = 0; i < fitnesscases; i++ ) {
        line   = in.readLine();
        tokens = new StringTokenizer(line);
        for (int j = 0; j <= varnumber; j++) {
          targets[i][j] = Double.parseDouble( tokens.nextToken().trim() );
        }
      }
      in.close();
    }
    catch(FileNotFoundException e) {
      System.out.println("ERROR: Data file not found");
      System.exit(0);
    }
    catch(Exception e ) {
      System.out.println("ERROR: Incorrect data format");
      System.exit(0);
    }
  }
  //
  // Crossover between 2 parents at randomly selected nodes
  //
  char [] crossover( char []parentA, char [] parentB ) {
    int lenA = Tree.traverse( parentA, 0 );
    int lenB = Tree.traverse( parentB, 0 );
    int xoAstart = rd.nextInt(lenA);
    int xoAend   = Tree.traverse( parentA, xoAstart );  
    int xoBstart = rd.nextInt(lenB);
    int xoBend   = Tree.traverse( parentB, xoBstart );
    int lenoff = xoAstart + (xoBend - xoBstart) + (lenA-xoAend);
    char [] offspring = new char[lenoff];
    System.arraycopy( parentA, 0, offspring, 0, xoAstart );
    System.arraycopy( parentB, xoBstart, offspring,xoAstart,(xoBend - xoBstart));
    System.arraycopy( parentA, xoAend, offspring,  xoAstart+(xoBend - xoBstart),
                         (lenA-xoAend) );
    return( offspring );
  }
  //
  // Mutate at randomly selected sites
  //
  char [] mutation( char [] parent, double pmut ) {
    int len = Tree.traverse( parent, 0 );
    int mutsite;
    char [] parentcopy = new char[len];
    System.arraycopy( parent, 0, parentcopy, 0, len );
    for( int i = 0; i < len; i ++ ) {  
      if ( rd.nextDouble() < pmut ) {
        mutsite =  i;
        if ( parentcopy[mutsite] < FSET_START )
          parentcopy[mutsite] = (char) rd.nextInt( varnumber + randomnumber );
        else
          switch( parentcopy[mutsite] ) {
            case ADD: 
            case SUB: 
            case MUL: 
            case DIV:
              parentcopy[mutsite] = (char) (rd.nextInt(FSET_END - FSET_START + 1)
                                              + FSET_START);
        }
      }
    }
    return( parentcopy );
  }
  //
  // Initialize population
  //
  public simple_gp( String fname, long s ) {
    fitness =  new double[POPSIZE];
    seed = s;
    if ( seed >= 0 ) rd.setSeed(seed);
    setup_gp( fname );
    // Create random constants
    for ( int i = 0; i < FSET_START; i++ )
      constants[i]= (maxrandom-minrandom)*rd.nextDouble()+minrandom;
    // Create a random population
    pop = Population.create( POPSIZE, DEPTH, fitness );
  }
  //
  // Entry for the evolutionary process
  //
  void evolve() {
    int gen = 0, indivs, offspring, parentA, parentB, parent;
    double newfit;
    char [] newind;
    Display.params();
    Display.stats( fitness, pop, 0 );
    for ( gen = 1; gen < MAXGENS; gen++ ) {
      if (  fbestpop > -1e-5 ) {
        System.out.print("*** Problem solved in "+gen+" generations ***\n");
        System.exit( 0 );
      }
      for ( indivs = 0; indivs < POPSIZE; indivs ++ ) {
        // Select fittest parents to reproduce,
        // either by corssover or mutation
        if ( rd.nextDouble() < CROSSOVER_PROB  ) {
          parentA = Tournament.positive( fitness, TSIZE );
          parentB = Tournament.positive( fitness, TSIZE );
          newind  = crossover( pop[parentA],pop[parentB] );
        }
        else {
          parent = Tournament.positive( fitness, TSIZE );
          newind = mutation( pop[parent], PMUT_PER_NODE );
        }
        newfit    = Population.fitness( newind );
        // Replace least fit individual
        offspring = Tournament.negative( fitness, TSIZE );
        pop[offspring]     = newind;
        fitness[offspring] = newfit;
      }
      Display.stats( fitness, pop, gen );
    }
    System.out.print("*** Problem NOT solved ***\n");
    System.exit( 1 );
  }
  //
  // *** Program Entry ***
  //
  public static void main( String[] args ) {
    String fname = "problem.dat";
    long s = -1;
    if ( args.length == 2 ) {
      s = Integer.valueOf(args[0]).intValue();
      fname = args[1];
    }
    if ( args.length == 1 ) {
      fname = args[0];
    }
    simple_gp gp = new simple_gp( fname, s );
    gp.evolve();
  }
};

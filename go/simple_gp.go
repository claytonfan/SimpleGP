//
// Program:   simple_gp.go
//
// Based on tiny_gp in java by
//   Riccardo Poli (email: rpoli@essex.ac.uk)
//
//
package main

import (
    "bufio"
    "fmt"
    "math"
    "math/rand"
    "os"
    "strconv"
    "strings"
    "time"
)

const (
    ADD        = 110
    SUB        = 111
    MUL        = 112
    DIV        = 113
    FSET_START = ADD
    FSET_END   = DIV
    MAX_LEN        = 10000
    POPSIZE        = 100000
    DEPTH          = 5
    GENERATIONS    = 100
    TSIZE          = 2
    PMUT_PER_NODE  = 0.05
    CROSSOVER_PROB = 0.9
)

var (
    fitness [POPSIZE]float64
    x                    [FSET_START]float64
    minrandom, maxrandom float64
    program              []byte
    PC                   int
    varnumber            int
    fitnesscases         int
    randomnumber         int
    fbestpop, favgpop    = 0.0, 0.0
    seed                 float64
    avg_len              float64
    targets              [][]float64
)
//
// Interpret program -- calculate 
//
func run() float64 {
    var primitive byte = program[PC]
    PC += 1
    if primitive < FSET_START {
        return (x[primitive])
    }
    switch primitive {
    case ADD:
        return (run() + run())
    case SUB:
        return (run() - run())
    case MUL:
        return (run() * run())
    case DIV:
        {
            var num, den float64 = run(), run()
            if math.Abs(den) <= 0.001 {
                return (num)
            } else {
                return (num / den)
            }
        }
    }
    return (0.0) // should never get here
}
//
// Traverse program tree
//
func traverse(buffer []byte, buffercount int) int {
    if buffer[buffercount] < FSET_START {
        buffercount += 1
        return (buffercount)
    }
    if buffer[buffercount] == ADD ||
        buffer[buffercount] == SUB ||
        buffer[buffercount] == MUL ||
        buffer[buffercount] == DIV {
        buffercount += 1
        return (traverse(buffer, traverse(buffer, buffercount)))
    }
    return (0) // should never get here
}
//
// Get values for fitness parameters and input data
//
func setup_fitness(fname string) {
    fp, _ := os.Open(fname)
    reader := bufio.NewReader(fp)
    str, _ := reader.ReadString('\n')
    var tokens = strings.Split(str, " ")
    varnumber, _ = strconv.Atoi(tokens[0])
    randomnumber, _ = strconv.Atoi(tokens[1])
    minrandom, _ = strconv.ParseFloat(tokens[2], 64)
    maxrandom, _ = strconv.ParseFloat(tokens[3], 64)
    fitnesscases, _ = strconv.Atoi(strings.Trim(tokens[4], " \r\n"))
    targets = make([][]float64, fitnesscases) // , varnumber+1)
    fmt.Printf("A - fitness cases =%d\n", fitnesscases)
    if varnumber+randomnumber >= FSET_START {
        fmt.Println("WARNING: Too many variables and constants")
    }
    var token2 [2]string
    for i := 0; i < fitnesscases; i++ {
        str, _ = reader.ReadString('\n')
        fmt.Sscanf(str, "%s %s\n", &(token2[0]), &(token2[1]))
        targets[i] = make([]float64, varnumber+1)
        for j := 0; j <= varnumber; j++ {
            targets[i][j], _ = strconv.ParseFloat(strings.Trim(token2[j], " "), 64)
        }
    }
    defer fp.Close()
}
//
// compute fitness for an individual program
//
func compute_fitness(Prog []byte) float64 {
    var result float64
    var fit float64 = 0.0
    _ = traverse(Prog, 0)
    for i := 0; i < fitnesscases; i++ {
        for j := 0; j < varnumber; j++ {
            x[j] = targets[i][j]
        }
        program = Prog
        PC = 0
        result = run()
        fit += math.Abs(result - targets[i][varnumber])
    }
    return (-fit)
}
//
// Grow a program
//
func grow(buffer []byte, pos int, max int, depth int) int {
    var prim byte = byte(rand.Intn(2))
    var one_child int
    if pos >= max {
        return (-1)
    }
    if pos == 0 {
        prim = 1
    }
    if prim == 0 || depth == 0 {
        prim = byte(rand.Intn(varnumber + randomnumber))
        buffer[pos] = prim
        return (pos + 1)
    } else {
        prim = byte(rand.Intn(FSET_END-FSET_START+1) + FSET_START)
        if prim == ADD || prim == SUB || prim == MUL || prim == DIV {
            buffer[pos] = prim
            one_child = grow(buffer, pos+1, max, depth-1)
            if one_child < 0 {
                return (-1)
            }
            return (grow(buffer, one_child, max, depth-1))
        }
    }
    return (0) // should never get here
}
//
// Display program
//
func print_indiv(buffer []byte, buffercounter int) int {
    var a1 = 0
    var a2 int
    if buffer[buffercounter] < FSET_START {
        if buffer[buffercounter] < byte(varnumber) {
            fmt.Print("X" + strconv.Itoa(int(buffer[buffercounter]+1)) + " ")
        } else {
            fmt.Printf("%f", x[buffer[buffercounter]])
        }
        buffercounter += 1
        return (buffercounter)
    }
    switch buffer[buffercounter] {
    case ADD:
        fmt.Print("(")
        buffercounter += 1
        a1 = print_indiv(buffer, buffercounter)
        fmt.Print(" + ")
    case SUB:
        fmt.Print("(")
        buffercounter += 1
        a1 = print_indiv(buffer, buffercounter)
        fmt.Print(" - ")
    case MUL:
        fmt.Print("(")
        buffercounter += 1
        a1 = print_indiv(buffer, buffercounter)
        fmt.Print(" * ")
    case DIV:
        fmt.Print("(")
        buffercounter += 1
        a1 = print_indiv(buffer, buffercounter)
        fmt.Print(" / ")
    }
    a2 = print_indiv(buffer, a1)
    fmt.Print(")")
    return (a2)
}
//
// Create an individual in population
//
func create_random_indiv(pop []byte, depth int) []byte {
    var prog [MAX_LEN]byte
    var indlen = grow(prog[:], 0, MAX_LEN, depth)
    for indlen < 0 {
        indlen = grow(prog[:], 0, MAX_LEN, depth)
    }
    ind := make([]byte, indlen)
    copy(ind, prog[:])
    return (ind)
}
//
// Create random population
//
func create_population(pop [][]byte, n int, depth int, fitness []float64) [][]byte {
    for i := 0; i < n; i++ {
        pop[i] = create_random_indiv(pop[i], depth)
        fitness[i] = compute_fitness(pop[i])
    }
    fmt.Println(" END - craete random pop\n")
    return (pop)
}
//
// Display population statistics
//
func stats(fitness []float64, pop [][]byte, gen int) {
    var best = rand.Intn(POPSIZE)
    var node_count = 0
    fbestpop = fitness[best]
    favgpop = 0.0
    for i := 0; i < POPSIZE; i++ {
        node_count += traverse(pop[i], 0)
        favgpop += fitness[i]
        if fitness[i] > fbestpop {
            best = i
            fbestpop = fitness[i]
        }
    }
    var avg_len float64 = float64(node_count / POPSIZE)
    favgpop /= POPSIZE
    fmt.Println(
        "Generation=", strconv.Itoa(gen),
        " Avg Fitness=", strconv.FormatFloat(-favgpop, 'E', -1, 64),
        " Best Fitness=", strconv.FormatFloat(-fbestpop, 'E', -1, 64),
        " Avg Size=", strconv.FormatFloat(avg_len, 'f', 5, 32),
        "\nBest Individual: ")
    print_indiv(pop[best], 0)
    fmt.Println("\n")
}
//
// Determine most fit
//
func tournament(fitness []float64, tsize int) int {
    var best int = rand.Intn(POPSIZE)
    var competitor int
    var fbest = -1.0e34
    for i := 0; i < tsize; i++ {
        competitor = rand.Intn(POPSIZE)
        if fitness[competitor] > fbest {
            fbest = fitness[competitor]
            best = competitor
        }
    }
    return (best)
}
//
// Determine least fit
//
func negative_tournament(fitness []float64, tsize int) int {
    var worst = rand.Intn(POPSIZE)
    var competitor int
    var fworst = 1e34
    for i := 0; i < tsize; i++ {
        competitor = rand.Intn(POPSIZE)
        if fitness[competitor] < fworst {
            fworst = fitness[competitor]
            worst = competitor
        }
    }
    return (worst)
}
//
// Crossover between 2 parents at randomly selected nodes
//
func crossover(parentA []byte, parentB []byte) []byte {
    var lenA, lenB = traverse(parentA, 0), traverse(parentB, 0)
    xoAstart := rand.Intn(lenA)
    xoAend   := traverse(parentA, xoAstart)
    xoBstart := rand.Intn(lenB)
    xoBend   := traverse(parentB, xoBstart)

    offspring := append(append(append([]byte{}, parentA[0       :xoAstart]...),
                                                parentB[xoBstart:xoBend]...),
                                                parentA[xoAend  :lenA]...)
    return (offspring)
}
//
// Mutate at randomly selected sites
//
func mutation(parent []byte, pmut float64) []byte {
    var len = traverse(parent, 0)
    var mutsite int
    parentcopy := make([]byte, len)
    copy(parentcopy, parent[0:len])
    for i := 0; i < len; i++ {
        if rand.Float64() < pmut {
            mutsite = i
            if parentcopy[mutsite] < FSET_START {
                parentcopy[mutsite] = byte(rand.Intn(varnumber + randomnumber))
            } else {
                if parentcopy[mutsite] == ADD ||
                   parentcopy[mutsite] == SUB ||
                    parentcopy[mutsite] == MUL ||
                    parentcopy[mutsite] == DIV {
                    parentcopy[mutsite] =
                        byte(rand.Intn(FSET_END-FSET_START+1) + FSET_START)
                }
            }
        }
    }
    return (parentcopy)
}
//
// Display GP parameters
//
func print_parms() {
    fmt.Println("-- Simple GP (Golang version) --")
    fmt.Printf("\nSeed             = %E", seed)
    fmt.Printf("\nMAX_LEN          = %d", MAX_LEN)
    fmt.Printf("\nPopulation       = %d", POPSIZE)
    fmt.Printf("\nDEPTH            = %d", DEPTH)
    fmt.Printf("\nCROSSOVER_PROB   = %f", CROSSOVER_PROB)
    fmt.Printf("\nPMUT_PER_NODE    = %f", PMUT_PER_NODE)
    fmt.Printf("\nMIN_RANDOM       = %f", minrandom)
    fmt.Printf("\nMAX_RANDOM       = %f", maxrandom)
    fmt.Printf("\nMax generations  = %d", GENERATIONS)
    fmt.Printf("\nFitness cases    = %d", fitnesscases)
    fmt.Printf("\nTSIZE            = %d", TSIZE)
    fmt.Printf("\n----------------------------------\n")
}
//
// Initialize population and start process
//
func simple_gp(fname string, s int64) {
    var pop = make([][]byte, POPSIZE, POPSIZE)
    var seed = s
    if seed >= 0 {
        rand.Seed(s)
    } else {
        rand.Seed(time.Now().UTC().UnixNano())
    }
    setup_fitness(fname)
    print_parms()
    for i := 0; i < FSET_START; i++ {
        x[i] = (maxrandom-minrandom)*rand.Float64() + minrandom
    }
    pop = create_population(pop, POPSIZE, DEPTH, fitness[:])
    evolve(pop)
}
//
// Entry for the evolutionary process
//
func evolve(pop [][]byte) {
    var offspring, parent1, parent2, parent int
    var newfit float64
    var newind []byte
    stats(fitness[:], pop, 0)
    for gen := 1; gen < GENERATIONS; gen++ {
        if fbestpop > -1e-5 {
            fmt.Println("Problem solved\n")
            os.Exit(0)
        }
        for indivs := 0; indivs < POPSIZE; indivs++ {
            if rand.Float64() < CROSSOVER_PROB {
                parent1 = tournament(fitness[:], TSIZE)
                parent2 = tournament(fitness[:], TSIZE)
                newind = crossover(pop[parent1], pop[parent2])
            } else {
                parent = tournament(fitness[:], TSIZE)
                newind = mutation(pop[parent], PMUT_PER_NODE)
            }
            newfit = compute_fitness(newind)
            offspring = negative_tournament(fitness[:], TSIZE)
            pop[offspring] = newind
            fitness[offspring] = newfit
        }
        stats(fitness[:], pop, gen)
    }
    fmt.Println("Problem not solved\n")
    os.Exit(1)
}
//
// *** Program *** Entry
//
func main() {
    var fname = "problem.dat"
    var s int64 = -1
    simple_gp(fname, s)
}

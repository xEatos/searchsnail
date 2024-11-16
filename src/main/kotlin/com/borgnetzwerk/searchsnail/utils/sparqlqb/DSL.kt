package com.borgnetzwerk.searchsnail.utils.sparqlqb

import javax.naming.directory.InvalidAttributesException

private const val tabSpaces = 4

private fun sp(depth: Int) = "".padStart(depth * tabSpaces, ' ')
private val sp1 = sp(1)

interface Prefixes {
    operator fun invoke(localPart: String): IRI
}

// @prefix ex: <http://www.example.org/> .
data class Namespace(val prefix: String, val localPart: String) : Prefixes {
    override fun invoke(localPart: String): IRI = IRI(this, localPart)

    companion object {
        val RDF = Namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
        val RDFS = Namespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
        val XSD = Namespace("xsd", "http://www.w3.org/2001/XMLSchema#")
        val FOAF = Namespace("foaf", "http://xmlns.com/foaf/0.1/")
        val PROP = Namespace("prop", "https://bnwiki.wikibase.cloud/prop/")
        val PROPT = Namespace("propt", "https://bnwiki.wikibase.cloud/prop/direct/")
        val PQUAL = Namespace("pqual", "https://bnwiki.wikibase.cloud/prop/qualifier/")
        val PSTAT = Namespace("pstat", "https://bnwiki.wikibase.cloud/prop/statement/")
        val ITEM = Namespace("item", "https://bnwiki.wikibase.cloud/entity/")
    }
}

// --- Terms

enum class TermType {
    IRI,
    BLANK_NODE,
    BLANK_NODE_TAIL,
    LITERAL,
    VARIABLE,
}

interface Term {
    fun getType(): TermType

    fun toString(depth: Int): String

    fun getPrefixes() : List<Namespace>
}



// ?var1, ?var2
data class Var(val variable: String) : Term {
    override fun getType(): TermType = TermType.VARIABLE

    override fun toString(): String = "?$variable"

    override fun getPrefixes(): List<Namespace> = listOf()

    override fun toString(depth: Int) = "?$variable".padStart(tabSpaces * depth, ' ')
}

// ex:Student, <http://www.example.org/Student>
data class IRI(val namespace: Namespace?, val relativePart: String) : Term {
    constructor(fullUrl: String) : this(null, fullUrl)

    override fun getType() = TermType.IRI

    override fun getPrefixes(): List<Namespace> = if(namespace != null) listOf(namespace) else listOf()

    override fun toString(): String {
        return if (namespace != null) {
            "${namespace.prefix}:$relativePart"
        } else {
            "<$relativePart>"
        }
    }

    override fun toString(depth: Int) = if (namespace != null) {
        "${namespace.prefix}:$relativePart".padStart(tabSpaces * depth, ' ')
    } else {
        "<$relativePart>".padStart(tabSpaces * depth, ' ')
    }
}

// "23"^^xsd:integer, "hello world", TODO: "Hallo Welt"@de
data class Literal<T>(val value: T, val iri: IRI?) : Term {
    constructor(value: T) : this(value, null)

    override fun getType(): TermType = TermType.LITERAL

    override fun getPrefixes(): List<Namespace> = if(iri?.namespace != null) listOf(iri.namespace) else listOf()

    override fun toString(): String =
        if (iri != null) {
            "\"$value\"^^$iri"
        } else {
            "$value"
        }

    override fun toString(depth: Int) = if (iri != null) {
        "\"$value\"^^$iri".padStart(tabSpaces * depth, ' ')
    } else {
        "$value".padStart(tabSpaces * depth, ' ')
    }
}

// RDF-Object == Tail
data class Tail(val term: Term) {
    override fun toString() = term.toString()
}

// RDF-Predicate
data class Predicate(val term: IRI) {
    override fun toString() = term.toString()

    companion object {
        fun isValidPredicate(term: Term): Boolean {
            return when (term.getType()) {
                TermType.IRI -> true
                TermType.VARIABLE -> true
                else -> false
            }
        }

        fun isValidPredicate(terms: List<Term>) = terms.fold(true) { acc, term -> acc && isValidPredicate(term) }

    }
}

// RDF-Subject
data class Subject(val term: Term) {

    companion object {
        fun isValidSubject(term: Term): Boolean {
            return when (term.getType()) {
                TermType.IRI -> true
                TermType.BLANK_NODE -> true
                TermType.VARIABLE -> true
                else -> false
            }
        }
    }

    override fun toString() = term.toString()
}

sealed class DSLError {
    data object IsNotASubject : DSLError()
    data object IsNotAPredicate : DSLError()
    data object IsNotATail : DSLError()
}

private fun List<*>.containsAllExact(other: List<*>): Boolean {
    if (this.size != other.size) return false
    var index = 0
    while (index < this.size) {
        if (this[index] != other[index]) return false
        index++
    }
    return true
}

private fun MutableList<Pair<List<Term>, MutableList<Term>>>.insert(
    props: List<Term>,
    tail: Term
): List<Term> {
    if (props.isEmpty()) throw InvalidAttributesException()
    this.find { pair ->
        pair.first.containsAllExact(props)
    }?.second?.add(tail) ?: this.add(Pair(props, mutableListOf(tail)))
    return props;
}

private fun MutableList<Pair<List<Term>, MutableList<Term>>>.mapPaths() = mapIndexed { index, pair ->
    pair.first.joinToString(separator = " / ", prefix = "    ", postfix = " ") +
            pair.second.joinToString(separator = ", ") { tail -> tail.toString(2) } +
            if (index == this.size - 1) "" else " ;"
}

private fun MutableList<Pair<List<Term>, MutableList<Term>>>.getPredicatesRecursively() = flatMap { pair ->
    val prefixOfPredicates = pair.first.flatMap { predPath ->
        predPath.getPrefixes()
    }
    val prefixOfTails = pair.second.flatMap { tail -> tail.getPrefixes() }
    prefixOfPredicates + prefixOfTails
}

// TODO merge BlankNode and BlankNodeTail, to normal class an factory to string based on provided content
// _:b12
data class BlankNode(val id: String) : Term {
    constructor() : this(getNext().toString())

    override fun getType(): TermType = TermType.BLANK_NODE

    override fun toString() = "_:b$id"

    override fun getPrefixes(): List<Namespace> = listOf()

    override fun toString(depth: Int) = "_:b$id".padStart(tabSpaces * depth, ' ')

    private companion object {
        private var counter: Int = 0
        private fun getNext() = counter++
    }
}

// ... [
//         ex:age "25" ;
//         ex:args [ ex:recursive _:b12 ] .
//     [
data class BlankNodeTail(val predicates: List<Term>, val tail: Term) : Term {
    constructor(predicate: Term, tail: Term) : this(listOf(predicate), tail)

    init {
        if (!Predicate.isValidPredicate(predicates)) {
            throw InvalidAttributesException()
        }
    }

    private var lastUsedPredicatePath = predicates // a path of length == 1 is like a normal triple pattern
    private var predicatePaths = mutableListOf(Pair(lastUsedPredicatePath, mutableListOf(tail)))

    fun with(tail: Term): BlankNodeTail {
        predicatePaths.insert(lastUsedPredicatePath, tail)
        return this
    }

    fun with(predicates: List<Term>, tail: Term): BlankNodeTail {
        if (!Predicate.isValidPredicate(predicates)) {
            throw InvalidAttributesException()
        }
        lastUsedPredicatePath = predicatePaths.insert(predicates, tail)
        return this
    }

    fun with(predicate: Term, tail: Term): BlankNodeTail {
        if (!Predicate.isValidPredicate(predicate)) {
            throw InvalidAttributesException()
        }
        lastUsedPredicatePath = predicatePaths.insert(listOf(predicate), tail)
        return this
    }

    override fun getType() = TermType.BLANK_NODE_TAIL

    override fun getPrefixes() = predicatePaths.getPredicatesRecursively()

    // add depth index? -> add to Term interface and pad every line based on depth
    override fun toString(): String {
        return predicatePaths.mapPaths().joinToString("\n", prefix = "[\n", postfix = "\n]")
    }

    override fun toString(depth: Int): String {
        return predicatePaths.mapIndexed { index, pair ->
            "".padStart(tabSpaces * depth, ' ') +
                    pair.first.joinToString(separator = " / ", prefix = "", postfix = " ") +
                    pair.second.joinToString(separator = ", ") { tail -> tail.toString(depth + 1) } +
                    if (index == predicatePaths.size - 1) "" else " ;"
        }.joinToString(
            "\n",
            prefix = "[\n",
            postfix = "\n" + "".padStart(tabSpaces * (depth - 1), ' ') + "]"
        )
    }

}

interface Graph {
    fun getType(): GraphStatement

    fun getPrefixes(): List<Namespace>
}

data class BasicGraphPattern(
    val subject: Term,
    val predicates: List<Term>,
    val tail: Term
) : Graph {
    constructor(subject: Term, predicate: Term, tail: Term) : this(subject, listOf(predicate), tail)

    init {
        if (!Subject.isValidSubject(subject) || !Predicate.isValidPredicate(predicates)) {
            throw InvalidAttributesException()
        }
    }

    private var lastUsedPredicatePath = predicates
    private var predicatePaths = mutableListOf(Pair(lastUsedPredicatePath, mutableListOf(tail)))

    fun add(predicate: Term, tail: Term): BasicGraphPattern {
        lastUsedPredicatePath = predicatePaths.insert(listOf(predicate), tail)
        return this
    }

    fun add(predicates: List<Term>, tail: Term): BasicGraphPattern {
        lastUsedPredicatePath = predicatePaths.insert(predicates, tail)
        return this
    }

    fun add(tail: Term): BasicGraphPattern {
        predicatePaths.insert(lastUsedPredicatePath, tail)
        return this
    }

    override fun getType() = GraphStatement.BasicGraph

    override fun getPrefixes() = predicatePaths.getPredicatesRecursively()

    override fun toString(): String {
        return predicatePaths.mapIndexed { index, pair ->
            pair.first.joinToString(separator = " / ", prefix = if (index == 0) "" else "    ", postfix = " ") +
                    pair.second.joinToString(separator = ", ") { tail -> tail.toString(1) } +
                    if (index == predicatePaths.size - 1) "" else " ;"
        }.joinToString("\n", prefix = "$subject ", postfix = " .")
    }

}

enum class GraphStatement {
    BasicGraph,
    Graph,
    UNION,
    VALUES,
    OPTIONAL,
    MINUS,
    FILTER,
    FILTER_NOT_EXIST,

}

data class Union(val gpLeft: GraphPattern, val gpRight: GraphPattern) : Graph {
    override fun toString() = "{\n$gpLeft}\nUNION\n{\n$gpRight}"

    override fun getPrefixes() = gpLeft.getPrefixes() + gpRight.getPrefixes()

    override fun getType() = GraphStatement.UNION
}

data class Minus(val gpLeft: GraphPattern, val gpRight: GraphPattern) : Graph {
    override fun toString() = "{\n$gpLeft}\nMINUS\n{\n$gpRight}"

    override fun getPrefixes() = gpLeft.getPrefixes() + gpRight.getPrefixes()

    override fun getType() = GraphStatement.MINUS
}

// UNDEF and multiple vars unsupported
data class Values(val variable: Var, val values: List<Term>) : Graph {
    override fun toString() = "VALUES $variable { \n$sp1${values.joinToString("\n$sp1",)} }"

    override fun getPrefixes() = values.flatMap { it.getPrefixes() }

    override fun getType() = GraphStatement.VALUES
}

data class Filter(val condition: String) : Graph {
    override fun toString() = "FILTER( $condition )"

    override fun getPrefixes() = emptyList<Namespace>()

    override fun getType() = GraphStatement.FILTER
}

data class Optional(val gp: GraphPattern) : Graph {
    override fun toString() = "OPTIONAL {\n$gp\n}"

    override fun getPrefixes() = gp.getPrefixes()

    override fun getType() = GraphStatement.OPTIONAL
}

data class FilterNotExists(val gp: GraphPattern) : Graph {
    override fun toString() = "FILTER NOT EXISTS {\n$gp\n}"

    override fun getPrefixes() = gp.getPrefixes()

    override fun getType() = GraphStatement.OPTIONAL
}


// e.g. {... {... FILTER} UNION {...} ... FILTER}
class GraphPattern() : Graph {
    private val gps = mutableListOf<Graph>()

    fun add(bgp: BasicGraphPattern): GraphPattern {
        gps.add(bgp)
        return this
    }

    fun addUnion(gpLeft: GraphPattern, gpRight: GraphPattern): GraphPattern {
        gps.add(Union(gpLeft, gpRight))
        return this
    }

    fun addMinus(gpLeft: GraphPattern, gpRight: GraphPattern): GraphPattern {
        gps.add(Minus(gpLeft, gpRight))
        return this
    }

    fun addValues(variable: Var, values: List<Term>): GraphPattern {
        gps.add(Values(variable, values))
        return this
    }

    fun addFilter(condition: String): GraphPattern {
        gps.add(Filter(condition))
        return this
    }

    fun addFilterNotExists(gp: GraphPattern): GraphPattern {
        gps.add(FilterNotExists(gp))
        return this
    }

    fun addOptional(gp: GraphPattern): GraphPattern {
        gps.add(Optional(gp))
        return this
    }

    // TODO add depth to Graph interface to support nested blocks
    override fun toString() = this.gps.joinToString("\n")

    override fun getPrefixes(): List<Namespace> = gps.flatMap { it.getPrefixes() }

    override fun getType() = GraphStatement.Graph
}


class DSL() {

    private var vars : List<Var>? = null
    private var gp : GraphPattern? = null
    private var limit : Int? = null
    private var offset : Int? = null
    private var prefixes  = setOf<Namespace>()
    private var orderBy : String? = null

    fun select(vararg vars: Var): DSL {
        this.vars = vars.toList()
        return this
    }

    fun where(gp: GraphPattern): DSL {
        this.gp = gp
        prefixes = gp.getPrefixes().toSet()
        return this
    }
    // overload where func to make nested queries?

    // modifiers
    fun groupBy(s: String): DSL {
        return this
    }

    fun having(s: String): DSL {
        return this
    }

    fun limit(limit: Int): DSL {
        TODO()
    }

    fun offset(offset: Int): DSL {
        TODO()
    }

    fun orderBy(s: String): DSL {
        orderBy = s
        return this
    }

    fun build() = listOf(
        prefixes.joinToString("\n", postfix = "\n") { prefix -> "PREFIX ${prefix.prefix}: <${prefix.localPart}>" },
        vars?.joinToString(" ", prefix = "SELECT ", postfix = "\n") ?: "",
        gp?.let { "WHERE {\n$it\n}\n" } ?: "",
        limit?.let { "LIMIT $it\n" } ?: "",
        offset?.let { "OFFSET $it\n" } ?: "",
        orderBy?.let { "ORDER BY $it\n" } ?: "",
    ).joinToString("")

}
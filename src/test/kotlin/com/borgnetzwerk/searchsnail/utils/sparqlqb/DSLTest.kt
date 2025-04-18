package com.borgnetzwerk.searchsnail.utils.sparqlqb

import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.Assertions.*
import org.junit.platform.commons.annotation.Testable

@Testable
class DSLTest : DescribeSpec({
    describe("queries") {

        /*
          PREFIX propt: <https://bnwiki.wikibase.cloud/prop/direct/>
          PREFIX item: <https://bnwiki.wikibase.cloud/entity/>
          PREFIX prop: <https://bnwiki.wikibase.cloud/prop/>
          PREFIX pqual: <https://bnwiki.wikibase.cloud/prop/qualifier/>
          PREFIX pstat: <https://bnwiki.wikibase.cloud/prop/statement/>

          SELECT ?channelName ?name ?start ?end ?pageid
          WHERE {
            ?media propt:P1 / propt:P9 item:Q5 ;
         rdfs:label ?channelName ;
         propt:P24 / prop:P20 ?sectionStatements  .
?sectionStatements pstat:P20 ?name ;
                pqual:P18 ?start ;
                pqual:P19 ?end ;
                pqual:P23 ?index .
            OPTIONAL {
                ?sectionStatements pqual:P27 ?pageid ;
            }
          }
        */
        it("query 1") {
            val item = Namespace.ITEM
            val propt = Namespace.PROPT
            val prop = Namespace.PROP
            val pstat = Namespace.PSTAT
            val rdfs = Namespace.RDFS

            val media = Var("media")
            val mediaName = Var("mediaName")
            val sectionName = Var("sectionName")

            val query = DSL()
                .select(mediaName, sectionName)
                .where(
                    GraphPattern()
                        .addValues(
                            media,
                            listOf(
                                IRI("https://bnwiki.wikibase.cloud/entity/Q6"),
                                IRI("https://bnwiki.wikibase.cloud/entity/Q3046")
                            )
                        )
                        .add(
                            BasicGraphPattern(media, listOf( propt("P1"), propt("P9")), item("Q5"))
                                .add(rdfs("label"), mediaName)
                        ).addOptional(
                            GraphPattern().add(
                                BasicGraphPattern(
                                    media,
                                    listOf(propt("P24"), prop("P20")),
                                    BlankNodeTail(pstat("P20"), sectionName)
                                )
                            )
                        )
                )

            println(query.build())
        }

        it("query 2") {
            val item = Namespace.ITEM
            val propt = Namespace.PROPT
            val prop = Namespace.PROP
            val pqual = Namespace.PQUAL
            val pstat = Namespace.PSTAT
            val rdfs = Namespace.RDFS

            val channelName = Var("channelName")
            val name = Var("name")
            val start = Var("start")
            val end = Var("end")
            val pageid = Var("pageid")
            val media = Var("media")
            val sectionStats = Var("sectionStatements")
            val index = Var("index")

            val query = DSL()
                .select(channelName, name, start, end, pageid)
                .where(
                    GraphPattern()
                        .add(
                            BasicGraphPattern(media, propt("P1"), item("Q5"))
                                .add(rdfs("label"), channelName)
                                .add(listOf(propt("P24"), prop("P20")), sectionStats)
                        ).add(
                            BasicGraphPattern(sectionStats, pstat("P20"), name)
                                .add(pqual("P18"), start)
                                .add(pqual("P19"), end)
                                .add(pqual("P23"), index)
                        ).addOptional(
                            GraphPattern().add(
                                BasicGraphPattern(sectionStats, pqual("P27"), pageid)
                            ).addOptional(
                                GraphPattern().add(
                                    BasicGraphPattern(sectionStats, pqual("P29"), pageid)
                                )
                            )
                        )
                )
                .orderBy("ASC($index)")
                .limit(10)
                .offset(10)

            println(query.build())
        }
    }
})
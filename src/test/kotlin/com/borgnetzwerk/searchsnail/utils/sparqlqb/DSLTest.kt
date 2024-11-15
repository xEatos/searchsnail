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
            ?media propt:P1 item:Q5 ;
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
            val item = Namespace("item", "https://bnwiki.wikibase.cloud/entity/")
            val propt = Namespace("propt", "https://bnwiki.wikibase.cloud/prop/direct/")
            val prop = Namespace("prop", "https://bnwiki.wikibase.cloud/prop/")
            val pqual = Namespace("pqual", "https://bnwiki.wikibase.cloud/prop/qualifier/")
            val pstat = Namespace("pstat", "https://bnwiki.wikibase.cloud/prop/statement/")
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
                        )
                    )
                ).orderBy("ASC($index)")

            println(query.build())
        }
    }
})
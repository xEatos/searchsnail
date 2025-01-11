package com.borgnetzwerk.searchsnail.domain.service

import com.borgnetzwerk.searchsnail.domain.model.Chapter
import com.borgnetzwerk.searchsnail.domain.model.MediumDuration
import com.borgnetzwerk.searchsnail.domain.model.VideoId
import com.borgnetzwerk.searchsnail.domain.model.YouTubeVideoData
import io.kotest.core.spec.style.DescribeSpec
import org.junit.platform.commons.annotation.Testable

@Testable
class YouTubeVideoDataServiceTest : DescribeSpec({
    describe("YoutubeVideoDataServiceTest") {
        it("should return a snippet"){
            val youtubeService = WebService().youTubeQueryService("as")
            try {
                val response = youtubeService.fetchVideos(listOf(VideoId("NHHUef7mrfY"), VideoId("kHd_cx1XdYk")))
                println(response.items.map { it -> YouTubeVideoData.resolve(it)?.chapters })
            } catch (e: Exception){
                println(e)
            }
        }
    }
})

@Testable
class ChaptersTest : DescribeSpec({

    it("test chapters") {

    val description = """
        Werbung: Ein Abo für die Natur: Die ersten 150 von euch erhalten den ersten Monat ihrer Planet Wild-Mitgliedschaft geschenkt. Nutzt diesen Link https://planetwild.com/r/doktorwhatson/join/12 oder den Code „WHATSON12". In ihrer aktuellen Mission verhindert Planet Wild das Fischen mit Schleppnetzen durch das Versenken von Skulpturen. Schaut's euch an: https://planetwild.com/r/doktorwhatson/m21/12

        Die Studie, um die es geht:
        https://www.nature.com/articles/s43247-024-01711-1

        Alle Quellen findet ihr in unserem Skript zum Video:
        https://docs.google.com/spreadsheets/d/1VMennV_5NpX-SEwO5SMHR-LCW-tjXfZzKFtJ7BUwIYw/edit?usp=sharing

        Hoss & Hopf haben einige Tage nach unserem Redaktionsschluss (10.12.24) und ohne Kontakt mit uns aufzunehmen eine Folge zu unserer Kritik veröffentlicht. Ihre Fehler gestehen sie größtenteils ein und geloben Besserung: https://youtu.be/cZ2E79z68SM 

        Unterstützt uns per Kanalmitgliedschaft direkt hier auf YouTube oder über Patreon: http://patreon.com/DoktorWhatson
        oder einmalig über PayPal: https://www.paypal.com/donate/?hosted_button_id=M9ALDL3JM8XWU
        Unser Spendenkonto:
        DE46 3705 0198 1936 5334 86
        BIC: COLSDE33

        Merch gibt’s hier: https://www.doktorwhatson.shop/
        Instagram: http://instagram.com/DoktorWhatson
        Twitter: http://www.twitter.com/DoktorWhatson
        Discord: http://discord.me/DoktorWhatson
         
        Kapitel:
        0:00 Reaction auf Hoss & Hopf
        1:23 Neue Studie über Klimawandel
        7:25 Keine Temperaturbeschleunigung?
        8:28 Der Begriff Gegenthese
        10:01 Der Mensch soll nichts machen
        11:14 Kraft der Erde
        12:34 Sind Kritiker Schwurbler?
        14:14 False Balancing
        17:07 Klimawandelleugnung
        19:10 Was sagt die Politik?
        20:27 Das Ozonloch soll gut sein?
        21:30 Viehzucht und der Klimawandel
        22:20 Was sagen Hoss & Hopf?
        22:40 Fazit
        23:30 Werbung: Planet Wild
        
    """.trimIndent()

    val duration = MediumDuration.of("24:46")!!
    println(duration)

    println(Chapter.resolveFromYouTubeDescription(description, duration).joinToString("\n"))
    }

})

@Testable
class RESTServiceTest: DescribeSpec({
    it("get statments for entity Q42", {
        val service = RESTService()
        val response = service.httpClientGetRequest()
        println(response)
    })
})
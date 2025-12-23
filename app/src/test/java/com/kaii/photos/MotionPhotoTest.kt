package com.kaii.photos

import com.kaii.photos.helpers.motion_photo.XmpMeta
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.Test

class MotionPhotoTest {
    // language=xml
    val xmpData = """
        <x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="Adobe XMP Core 5.1.0-jc003">
          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description rdf:about=""
                xmlns:hdrgm="http://ns.adobe.com/hdr-gain-map/1.0/"
                xmlns:xmpNote="http://ns.adobe.com/xmp/note/"
                xmlns:Container="http://ns.google.com/photos/1.0/container/"
                xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
                xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
              hdrgm:Version="1.0"
              xmpNote:HasExtendedXMP="CD5B127D1CEF778D087CB7B28519260F"
              GCamera:MotionPhoto="1"
              GCamera:MotionPhotoVersion="1"
              GCamera:MotionPhotoPresentationTimestampUs="1166704">
              <Container:Directory>
                <rdf:Seq>
                  <rdf:li rdf:parseType="Resource">
                    <Container:Item
                      Item:Semantic="Primary"
                      Item:Mime="image/jpeg"/>
                  </rdf:li>
                  <rdf:li rdf:parseType="Resource">
                    <Container:Item
                      Item:Length="20890"
                      Item:Mime="image/jpeg"
                      Item:Semantic="GainMap"/>
                  </rdf:li>
                  <rdf:li rdf:parseType="Resource">
                    <Container:Item
                      Item:Mime="video/mp4"
                      Item:Semantic="MotionPhoto"
                      Item:Length="3022367"
                      Item:Padding="0"/>
                  </rdf:li>
                </rdf:Seq>
              </Container:Directory>
            </rdf:Description>
          </rdf:RDF>
        </x:xmpmeta>
    """.trimIndent()

    @Suppress("DEPRECATION")
    @OptIn(ExperimentalXmlUtilApi::class)
    @Test
    fun parseXmpData() {
        val serializer = serializer<XmpMeta>()
        val xml = XML {
            autoPolymorphic = true

            // ignore unknown keys
            unknownChildHandler = UnknownChildHandler { _, _, _, _, _ ->
                emptyList()
            }
        }
        val data = xml.decodeFromString(serializer, xmpData)

        println(data.toString())
    }
}
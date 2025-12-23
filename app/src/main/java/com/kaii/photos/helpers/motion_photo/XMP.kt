package com.kaii.photos.helpers.motion_photo

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlSerialName

object Namespaces {
    const val X = "adobe:ns:meta/"
    const val RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    const val HDRGM = "http://ns.adobe.com/hdr-gain-map/1.0/"
    const val XMP_NOTE = "http://ns.adobe.com/xmp/note/"
    const val CONTAINER = "http://ns.google.com/photos/1.0/container/"
    const val ITEM = "http://ns.google.com/photos/1.0/container/item/"
    const val GCAMERA = "http://ns.google.com/photos/1.0/camera/"
}

@Serializable
@XmlSerialName("xmpmeta", Namespaces.X, "x")
data class XmpMeta(
    @XmlSerialName("xmptk", Namespaces.X, "x")
    val xmptk: String,

    @XmlSerialName("RDF", Namespaces.RDF, "rdf")
    val rdf: Rdf
)

@Serializable
@XmlSerialName("RDF", Namespaces.RDF, "rdf")
data class Rdf(
    @XmlSerialName("Description", Namespaces.RDF, "rdf")
    val description: Description
)

@Serializable
@XmlSerialName("Description", Namespaces.RDF, "rdf")
data class Description(
    @XmlSerialName("about", Namespaces.RDF, "rdf")
    val about: String = "",

    @XmlSerialName("Version", Namespaces.HDRGM, "hdrgm")
    val hdrgmVersion: String? = null,

    @XmlSerialName("HasExtendedXMP", Namespaces.XMP_NOTE, "xmpNote")
    val hasExtendedXMP: String? = null,

    @XmlSerialName("MotionPhoto", Namespaces.GCAMERA, "GCamera")
    val motionPhoto: Int? = null,

    @XmlSerialName("MotionPhotoVersion", Namespaces.GCAMERA, "GCamera")
    val motionPhotoVersion: Int? = null,

    @XmlSerialName("MotionPhotoPresentationTimestampUs", Namespaces.GCAMERA, "GCamera")
    val motionPhotoTimestamp: Long? = null,

    @XmlSerialName("Directory", Namespaces.CONTAINER, "Container")
    val directory: Directory? = null
)

@Serializable
@XmlSerialName("Directory", Namespaces.CONTAINER, "Container")
data class Directory(
    @XmlSerialName("Seq", Namespaces.RDF, "rdf")
    val seq: Seq
)

@Serializable
@XmlSerialName("Seq", Namespaces.RDF, "rdf")
data class Seq(
    @XmlSerialName("li", Namespaces.RDF, "rdf")
    val items: List<ListItem>
)

@Serializable
@XmlSerialName("li", Namespaces.RDF, "rdf")
data class ListItem(
    @XmlSerialName("parseType", Namespaces.RDF, "rdf")
    val parseType: String? = null,

    @XmlSerialName("Item", Namespaces.CONTAINER, "Container")
    val item: ContainerItem
)

@Serializable
@XmlSerialName("Item", Namespaces.CONTAINER, "Container")
data class ContainerItem(
    @XmlSerialName("Semantic", Namespaces.ITEM, "Item")
    val semantic: String? = null,

    @XmlSerialName("Mime", Namespaces.ITEM, "Item")
    val mime: String? = null,

    @XmlSerialName("Length", Namespaces.ITEM, "Item")
    val length: Int? = null,

    @XmlSerialName("Padding", Namespaces.ITEM, "Item")
    val padding: Int? = null
)
package biouml.plugins.riboseq.ingolia;

public enum StartSiteType
{
    CANONICAL, //Matches known CDS
    TRUNCATION, //Leads to N-terminus truncation
    EXTENSION, //Leads to N-terminus extension
    INTERNAL_OUT_OF_FRAME, //Inside known CDS and out of frame
    UPSTREAM_OVERLAPPING, //Predicted CDS overlaps known CDS and predicted start site upstream of known (out of frame, in case of in-frame it is EXTENSION)
    UPSTREAM, // Predicted CDS completely upstream of known CDS
    DOWNSTREAM, //Completely downstream of known CDS
    NOVEL, //Predicted on transcript without known CDS
}

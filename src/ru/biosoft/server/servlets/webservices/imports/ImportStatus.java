package ru.biosoft.server.servlets.webservices.imports;

public enum ImportStatus
{
    UPLOAD_CREATED, //User select files for upload, but upload not yet started
    UPLOADING,
    UPLOAD_FINISHED,
    UPLOAD_ERROR,
    FORMAT_DETECTED, // The list of possible formats has been created
    FORMAT_SET, // User select format among possible formats and set options for this format
    IMPORTING,
    DONE,
    IMPORT_ERROR
}

/**
 * OpenTok Java SDK
 * Copyright (C) 2018 TokBox, Inc.
 * http://www.tokbox.com
 *
 * Licensed under The MIT License (MIT). See LICENSE file for more information.
 */
package com.opentok.util;

import com.opentok.exception.RequestException;
import org.asynchttpclient.Response;

public class ClientResponseUtils {

    public static String parseCreateSessionResponse(Response response) throws RequestException {
        String responseString = null;

        switch (response.getStatusCode()) {
            case 200:
                responseString = response.getResponseBody();
                break;
            default:
                throw new RequestException("Could not create an OpenTok Session. The server response was invalid." +
                        " response code: " + response.getStatusCode());
        }
        return responseString;
    }

    public static String parseGetArchiveResponse(String archiveId, Response response) throws RequestException {
        String responseString = null;
        switch (response.getStatusCode()) {
            case 200:
                responseString = response.getResponseBody();
                break;
            case 400:
                throw new RequestException("Could not get an OpenTok Archive. The archiveId was invalid. " +
                        "archiveId: " + archiveId);
            case 403:
                throw new RequestException("Could not get an OpenTok Archive. The request was not authorized.");
            case 500:
                throw new RequestException("Could not get an OpenTok Archive. A server error occurred.");
            default:
                throw new RequestException("Could not get an OpenTok Archive. The server response was invalid." +
                        " response code: " + response.getStatusCode());
        }

        return responseString;
    }

    public static String parseGetArchivesResponse(Response response) throws RequestException {
        String responseString = null;

        switch (response.getStatusCode()) {
            case 200:
                responseString = response.getResponseBody();
                break;
            case 403:
                throw new RequestException("Could not get OpenTok Archives. The request was not authorized.");
            case 500:
                throw new RequestException("Could not get OpenTok Archives. A server error occurred.");
            default:
                throw new RequestException("Could not get an OpenTok Archive. The server response was invalid." +
                        " response code: " + response.getStatusCode());
        }

        return responseString;
    }

    public static String parseStartArchiveResponse(String sessionId, Response response) throws RequestException {
        String responseString = null;

        switch (response.getStatusCode()) {
            case 200:
                responseString = response.getResponseBody();
                break;
            case 403:
                throw new RequestException("Could not start an OpenTok Archive. The request was not authorized.");
            case 404:
                throw new RequestException("Could not start an OpenTok Archive. The sessionId does not exist. " +
                        "sessionId = " + sessionId);
            case 409:
                throw new RequestException("Could not start an OpenTok Archive. The session is either " +
                        "peer-to-peer or already recording. sessionId = " + sessionId);
            case 500:
                throw new RequestException("Could not start an OpenTok Archive. A server error occurred.");
            default:
                throw new RequestException("Could not start an OpenTok Archive. The server response was invalid." +
                        " response code: " + response.getStatusCode());
        }

        return responseString;
    }

    public static String parseStopArchiveResponse(String archiveId, Response response) throws RequestException {
        String responseString = null;

        switch (response.getStatusCode()) {
            case 200:
                responseString = response.getResponseBody();
                break;
            case 400:
                // NOTE: the REST api spec talks about sessionId and action, both of which aren't required.
                //       see: https://github.com/opentok/OpenTok-2.0-archiving-samples/blob/master/REST-API.md#stop_archive
                throw new RequestException("Could not stop an OpenTok Archive.");
            case 403:
                throw new RequestException("Could not stop an OpenTok Archive. The request was not authorized.");
            case 404:
                throw new RequestException("Could not stop an OpenTok Archive. The archiveId does not exist. " +
                        "archiveId = " + archiveId);
            case 409:
                throw new RequestException("Could not stop an OpenTok Archive. The archive is not being recorded. " +
                        "archiveId = " + archiveId);
            case 500:
                throw new RequestException("Could not stop an OpenTok Archive. A server error occurred.");
            default:
                throw new RequestException("Could not stop an OpenTok Archive. The server response was invalid." +
                        " response code: " + response.getStatusCode());
        }

        return responseString;
    }

    public static String parseDeleteArchiveResponse(String archiveId, Response response) throws RequestException {
        String responseString = null;

        switch (response.getStatusCode()) {
            case 204:
                responseString = response.getResponseBody();
                break;
            case 403:
                throw new RequestException("Could not delete an OpenTok Archive. The request was not authorized.");
            case 409:
                throw new RequestException("Could not delete an OpenTok Archive. The status was not \"uploaded\"," +
                        " \"available\", or \"deleted\". archiveId = " + archiveId);
            case 500:
                throw new RequestException("Could not delete an OpenTok Archive. A server error occurred.");
            default:
                throw new RequestException("Could not get an OpenTok Archive. The server response was invalid." +
                        " response code: " + response.getStatusCode());
        }

        return responseString;
    }
}

package com.powsybl.afs.ws.client.utils.st;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.powsybl.afs.storage.AfsStorageException;

public class ClientUtilsSt {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientUtilsSt.class);
    public static void checkOk(ResponseEntity<?> response) {
        int status = response.getStatusCode().value();
        if (status != HttpStatus.OK.value()) {
            if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                throw createServerErrorException(response);
            } else {
                throw createUnexpectedResponseStatus(response.getStatusCode());
            }
        }
    }
    private static AfsStorageException createServerErrorException(ResponseEntity response) {
        return new AfsStorageException(response.getBody().toString());
    }
    private static AfsStorageException createUnexpectedResponseStatus(HttpStatus status) {
        return new AfsStorageException("Unexpected response status: '" + status + "'");
    }
    public static <T> T readEntityIfOk(ResponseEntity<T> response) {
        int status = response.getStatusCode().value();
        if (status == HttpStatus.OK.value()) {
            T entity = (T) response.getBody();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("    --> {}", entity);
            }
            return entity;
        } else if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            throw createServerErrorException(response);
        } else {
            throw createUnexpectedResponseStatus(response.getStatusCode());
        }
    }
}

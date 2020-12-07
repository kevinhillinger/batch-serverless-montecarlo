package org.kevinhillinger.batch;

import com.microsoft.azure.batch.BatchClient;
import com.microsoft.azure.batch.auth.BatchSharedKeyCredentials;

public class BatchClientFactory {

    public static BatchClient create(BatchClientSettings settings) {
        BatchSharedKeyCredentials credentials = settings.asCredentials();
        BatchClient client = BatchClient.open(credentials);

        return client;
    }
}

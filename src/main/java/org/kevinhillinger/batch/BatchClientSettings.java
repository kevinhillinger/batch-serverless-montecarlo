package org.kevinhillinger.batch;

import com.microsoft.azure.batch.auth.BatchSharedKeyCredentials;

public class BatchClientSettings {
    public String account;
    public String endpoint;
    public String key;

    public BatchSharedKeyCredentials asCredentials() {
        return new BatchSharedKeyCredentials(endpoint, account, key);
    }
}

package com.azure;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import java.util.Optional;

import com.azure.storage.blob.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class secureFunction {

    private final static String keyVaultUri = "https://kwikssecurevault.vault.azure.net";
    @FunctionName("getBlob")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String blobName = request.getQueryParameters().get("bloName");

        if (blobName == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a blobName on the query string").build();
        } else {
            KeyVaultSecret retrievedSecret = secretBuilder().getSecret("storeCredentials");
            String connectionString = retrievedSecret.getValue();
            String fileContent = readBlobAsString(connectionString,"data",blobName);
            return request.createResponseBuilder(HttpStatus.OK).body(fileContent).build();
        }
    }

    private SecretClient secretBuilder() {

        SecretClient secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        return secretClient;
    }

    private String readBlobAsString(String connectionString, String containerName, String blobName){
        BlobServiceClient blobServiceClient= new BlobServiceClientBuilder()
                .endpoint("https://kwiksstore.blob.core.windows.net/")
                .connectionString(connectionString)
                .buildClient();

       BlobContainerClient containerClient = blobServiceClient
                .getBlobContainerClient(containerName);
       BlobClient blobClient = containerClient
                .getBlobClient(blobName);
       return blobClient.downloadContent().toString();
    }
}

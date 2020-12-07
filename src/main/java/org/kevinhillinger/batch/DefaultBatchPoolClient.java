package org.kevinhillinger.batch;

import java.io.*;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import com.microsoft.azure.batch.*;
import com.microsoft.azure.batch.auth.*;
import com.microsoft.azure.batch.protocol.models.*;

public class DefaultBatchPoolClient {

    private BatchClient client;

    public DefaultBatchPoolClient(BatchClient client) {
        this.client = client;
    }

    /**
     * Create IaaS pool if pool isn't exist
     *
     * @param poolId
     *            the pool id
     * @return the pool instance
     * @throws BatchErrorException
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    private CloudPool createPoolIfNotExists(String poolId)
            throws BatchErrorException, IllegalArgumentException, IOException, InterruptedException, TimeoutException {
        // Create a pool with 1 A1 VM
        String osPublisher = "OpenLogic";
        String osOffer = "CentOS";
        String poolVMSize = "STANDARD_A1";
        int poolVMCount = 1;
        Duration POOL_STEADY_TIMEOUT = Duration.ofMinutes(5);
        Duration VM_READY_TIMEOUT = Duration.ofMinutes(20);

        // Check if pool exists
        if (!client.poolOperations().existsPool(poolId)) {

            // See detail of creating IaaS pool at
            // https://blogs.technet.microsoft.com/windowshpc/2016/03/29/introducing-linux-support-on-azure-batch/
            // Get the sku image reference
            List<ImageInformation> skus = client.accountOperations().listSupportedImages();
            String skuId = null;
            ImageReference imageRef = null;

            for (ImageInformation sku : skus) {
                if (sku.osType() == OSType.LINUX) {
                    if (sku.verificationType() == VerificationType.VERIFIED) {
                        if (sku.imageReference().publisher().equalsIgnoreCase(osPublisher)
                                && sku.imageReference().offer().equalsIgnoreCase(osOffer)) {
                            imageRef = sku.imageReference();
                            skuId = sku.nodeAgentSKUId();
                            break;
                        }
                    }
                }
            }

            // Use IaaS VM with Linux
            VirtualMachineConfiguration configuration = new VirtualMachineConfiguration();
            configuration.withNodeAgentSKUId(skuId).withImageReference(imageRef);

            client.poolOperations().createPool(poolId, poolVMSize, configuration, poolVMCount);
        }

        long startTime = System.currentTimeMillis();
        long elapsedTime = 0L;
        boolean steady = false;

        // Wait for the VM to be allocated
        while (elapsedTime < POOL_STEADY_TIMEOUT.toMillis()) {
            CloudPool pool = client.poolOperations().getPool(poolId);
            if (pool.allocationState() == AllocationState.STEADY) {
                steady = true;
                break;
            }

            System.out.println("wait 30 seconds for pool steady...");
            Thread.sleep(30 * 1000);
            elapsedTime = (new Date()).getTime() - startTime;
        }

        if (!steady) {
            throw new TimeoutException("The pool did not reach a steady state in the allotted time");
        }

        return client.poolOperations().getPool(poolId);
    }

    /**
     * The VMs in the pool don't need to be in and IDLE state in order to submit a job. The following is an
     * example of how to poll for the VM state
     * @param poolId
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws IOException
     */
    private void arePoolMachinesReady(String poolId) throws InterruptedException, TimeoutException, IOException {
        Duration VM_READY_TIMEOUT = Duration.ofMinutes(20);

        // The VMs in the pool don't need to be in and IDLE state in order to submit a
        // job.
        // The following code is just an example of how to poll for the VM state
        long startTime = System.currentTimeMillis();
        long elapsedTime = 0L;
        boolean hasIdleVM = false;

        // Wait for at least 1 VM to reach the IDLE state
        while (elapsedTime < VM_READY_TIMEOUT.toMillis()) {
            List<ComputeNode> poolNodes = getPoolNodes(poolId);

            if (!poolNodes.isEmpty()) {
                hasIdleVM = true;
                break;
            }

            System.out.println("wait 30 seconds for VM start...");
            Thread.sleep(30 * 1000);
            elapsedTime = (new Date()).getTime() - startTime;
        }

        if (!hasIdleVM) {
            throw new TimeoutException("The node did not reach an IDLE state in the allotted time");
        }

    }

    private List<ComputeNode> getPoolNodes(String poolId) throws IOException {
        return client.computeNodeOperations().listComputeNodes(poolId,
                new DetailLevel.Builder().withSelectClause("id, state").withFilterClause("state eq 'idle'")
                        .build());
    }

}

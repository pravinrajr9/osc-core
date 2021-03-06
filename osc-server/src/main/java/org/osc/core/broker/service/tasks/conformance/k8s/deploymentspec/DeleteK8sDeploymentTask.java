/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.service.tasks.conformance.k8s.deploymentspec;

import java.util.Set;

import javax.persistence.EntityManager;

import org.osc.core.broker.job.lock.LockObjectReference;
import org.osc.core.broker.model.entities.virtualization.openstack.DeploymentSpec;
import org.osc.core.broker.rest.client.k8s.KubernetesClient;
import org.osc.core.broker.rest.client.k8s.KubernetesDeploymentApi;
import org.osc.core.broker.service.persistence.OSCEntityManager;
import org.osc.core.broker.service.tasks.TransactionalTask;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DeleteK8sDeploymentTask.class)
public class DeleteK8sDeploymentTask extends TransactionalTask {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteK8sDeploymentTask.class);

    private DeploymentSpec ds;

    private KubernetesDeploymentApi k8sDeploymentApi;

    public DeleteK8sDeploymentTask create(DeploymentSpec ds) {
        return create(ds, null);
    }

    DeleteK8sDeploymentTask create(DeploymentSpec ds, KubernetesDeploymentApi k8sDeploymentApi) {
        DeleteK8sDeploymentTask task = new DeleteK8sDeploymentTask();
        task.dbConnectionManager = this.dbConnectionManager;
        task.txBroadcastUtil = this.txBroadcastUtil;
        task.ds = ds;
        task.k8sDeploymentApi = k8sDeploymentApi;
        return task;
    }

    @Override
    public void executeTransaction(EntityManager em) throws Exception {
        OSCEntityManager<DeploymentSpec> dsEmgr = new OSCEntityManager<DeploymentSpec>(DeploymentSpec.class, em, this.txBroadcastUtil);
        this.ds = dsEmgr.findByPrimaryKey(this.ds.getId());

        try (KubernetesClient client = new KubernetesClient(this.ds.getVirtualSystem().getVirtualizationConnector())) {
            if (this.k8sDeploymentApi == null) {
                this.k8sDeploymentApi = new KubernetesDeploymentApi(client);
            } else {
                this.k8sDeploymentApi.setKubernetesClient(client);
            }

            this.k8sDeploymentApi.deleteDeployment(this.ds.getExternalId(), this.ds.getNamespace(), K8sUtil.getK8sName(this.ds));
            LOG.info(String.format("Deleted the deployment in kubernetes with name '%s' and id '%s'.", K8sUtil.getK8sName(this.ds), this.ds.getExternalId()));
        }
    }

    @Override
    public String getName() {
        return String.format("Deleting the K8s Deployment Spec '%s'", K8sUtil.getK8sName(this.ds));
    }

    @Override
    public Set<LockObjectReference> getObjects() {
        return LockObjectReference.getObjectReferences(this.ds);
    }
}

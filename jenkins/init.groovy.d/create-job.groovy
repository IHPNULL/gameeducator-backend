// Cria (ou atualiza) os 3 jobs de pipeline do GameEducator a partir do
// Jenkinsfile de cada repositorio, toda vez que o Jenkins sobe:
//   gameeducator-backend, gameeducator-frontend, gameeducator-automation
// O job de automacao dispara sozinho quando o backend OU o frontend termina com sucesso.
//
// Por que um script Groovy, e nao "Job DSL" via jenkins/casc.yaml: o Job DSL
// roda dentro do sandbox de seguranca do Jenkins, que bloqueia leitura de
// arquivo arbitrario (new File(...).text) sem uma aprovacao manual de
// administrador - o que quebraria a subida automatica que este projeto
// precisa. Um script em init.groovy.d roda com confianca total (e o mesmo
// nivel de confianca de quem escreve este arquivo no repositorio), sem essa
// restricao, e e a forma padrao e documentada de bootstrap do Jenkins.
//
// Este arquivo e copiado para dentro da imagem (ver jenkins/Dockerfile) e o
// Jenkins o copia para o volume de dados so na PRIMEIRA subida (comportamento
// padrao da imagem oficial). Se voce alterar este script depois, rode
// "docker compose down -v" antes de subir de novo para garantir que a versao
// nova seja usada (ver jenkins/README.md).

import hudson.model.Result
import jenkins.model.Jenkins
import jenkins.triggers.ReverseBuildTrigger
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition

def jenkinsInstance = Jenkins.get()

// LOCAL_SOURCES_ROOT e onde a pasta com os 3 repositorios fica montada
// somente-leitura dentro do container do Jenkins (ver jenkins/docker-compose.yml).
def sourcesRoot = System.getenv('LOCAL_SOURCES_ROOT') ?: '/src'
def jobNames = ['gameeducator-backend', 'gameeducator-frontend', 'gameeducator-automation']

jobNames.each { jobName ->
    def jenkinsfile = new File("${sourcesRoot}/${jobName}/Jenkinsfile")

    if (!jenkinsfile.exists()) {
        println "[create-job] AVISO: ${jenkinsfile} nao encontrado. O job '${jobName}' NAO foi criado/atualizado."
        println "[create-job] Confira SOURCES_DIR em jenkins/docker-compose.yml (deve conter a pasta ${jobName})."
        return
    }

    def job = jenkinsInstance.getItem(jobName)
    if (job == null) {
        job = jenkinsInstance.createProject(WorkflowJob.class, jobName)
        println "[create-job] Job '${jobName}' criado a partir de ${jenkinsfile}."
    } else {
        println "[create-job] Job '${jobName}' ja existia; atualizando a definicao com o Jenkinsfile atual (${jenkinsfile})."
    }

    job.setDefinition(new CpsFlowDefinition(jenkinsfile.text, true))

    if (jobName == 'gameeducator-automation') {
        // dispara depois que o backend ou o frontend termina com sucesso
        def existente = job.triggers.values().find { it instanceof ReverseBuildTrigger }
        if (existente != null) {
            job.removeTrigger(existente.descriptor)
        }
        job.addTrigger(new ReverseBuildTrigger('gameeducator-backend,gameeducator-frontend', Result.SUCCESS))
    }
    job.save()
}
jenkinsInstance.save()

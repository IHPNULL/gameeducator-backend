// Cria (ou atualiza) o job de pipeline "gameeducator" a partir do Jenkinsfile
// do repositorio, toda vez que o Jenkins sobe.
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

import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition

def jenkinsInstance = Jenkins.get()
def jobName = 'gameeducator'

// LOCAL_SOURCE_DIR e onde o repositorio local (sem Git) fica montado
// somente-leitura dentro do container do Jenkins (ver jenkins/docker-compose.yml).
def sourceDir = System.getenv('LOCAL_SOURCE_DIR') ?: '/src'
def jenkinsfile = new File("${sourceDir}/Jenkinsfile")

if (!jenkinsfile.exists()) {
    println "[create-job] AVISO: ${jenkinsfile} nao encontrado. O job '${jobName}' NAO foi criado/atualizado."
    println "[create-job] Confira o volume montado em jenkins/docker-compose.yml (servico jenkins, caminho /src)."
    return
}

def jenkinsfileContent = jenkinsfile.text
def job = jenkinsInstance.getItem(jobName)

if (job == null) {
    job = jenkinsInstance.createProject(WorkflowJob.class, jobName)
    println "[create-job] Job '${jobName}' criado a partir de ${jenkinsfile}."
} else {
    println "[create-job] Job '${jobName}' ja existia; atualizando a definicao com o Jenkinsfile atual (${jenkinsfile})."
}

job.setDefinition(new CpsFlowDefinition(jenkinsfileContent, true))
job.save()
jenkinsInstance.save()

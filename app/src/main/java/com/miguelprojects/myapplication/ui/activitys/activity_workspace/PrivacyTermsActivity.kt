package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityPrivacyTermsBinding
import com.miguelprojects.myapplication.factory.CitizenViewModelFactory
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class PrivacyTermsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyTermsBinding
    private lateinit var database: MyAppDatabase
    private var userModel = UserModel()
    private var workspaceModel = WorkspaceModel()
    private var citizenModel = CitizenModel()
    private var userId: String = ""
    private var workspaceId: String? = null
    private var citizenId: String? = null
    private var pendingTasks = 0
    private lateinit var supportName: String
    private lateinit var supportEmail: String

    private val userViewModel: UserViewModel by lazy {
        val userDao = database.userDao()
        val userRepository = UserRepository(userDao)
        val userFactory = UserViewModelFactory(userRepository)
        ViewModelProvider(this, userFactory)[UserViewModel::class.java]
    }
    private val workspaceViewModel: WorkspaceViewModel by lazy {
        val workspaceDao = database.workspaceDao()
        val workspaceRepository = WorkspaceRepository(workspaceDao)
        val workspaceFactory = WorkspaceViewModelFactory(workspaceRepository)
        ViewModelProvider(this, workspaceFactory)[WorkspaceViewModel::class.java]
    }

    private val citizenViewModel: CitizenViewModel by lazy {
        val citizenDao = database.citizenDao()
        val citizenRepository = CitizenRepository(citizenDao)
        val citizenFactory = CitizenViewModelFactory(citizenRepository)
        ViewModelProvider(this, citizenFactory)[CitizenViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPrivacyTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        println("Chegou no privacy")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database

        getExtraData()

        DrawerConfigurator(
            this,
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        loadModelValues()

        loadPrivacyTerm()
    }

    private fun getExtraData() {
        userId = intent.getStringExtra("user_id") ?: ""
        workspaceId = intent.getStringExtra("workspace_id")
        citizenId = intent.getStringExtra("citizen_id")

        supportName = getString(R.string.support_name)
        supportEmail = getString(R.string.support_email)

        println(userId)
        println(workspaceId)
        println(citizenId)
    }

    private fun loadModelValues() {
        if (!workspaceId.isNullOrEmpty()) {
            loadWorkspaceModel()
        } else {
            loadUserModel(userId)
        }

        if (!citizenId.isNullOrEmpty()) {
            loadCitizenModel()
        }
    }

    private fun loadUserModel(userId: String) {
        pendingTasks++ // Iniciando a tarefa de carregar o usuário

        if (NetworkChangeReceiver().isNetworkConnected(this)) {
            userViewModel.userModel.observe(this, Observer { user ->
                userModel = user
                taskCompleted()
            })
            userViewModel.loadUserModel(userId)
        } else {
            userViewModel.loadUserRoom(userId) { user ->
                if (user == null) {
                    toastMessage("Erro ao carregar os dados! Tente novamente!")
                    finish()
                    return@loadUserRoom
                }

                userModel = User.toUserModel(user)

                taskCompleted() // Indica que a tarefa de carregar o usuário foi concluída
            }
        }
    }

    private fun loadWorkspaceModel() {
        pendingTasks++ // Iniciando a tarefa de carregar o workspace
        workspaceViewModel.loadDataRoom(workspaceId!!) { workspace ->
            if (workspace == null) {
                toastMessage("Erro ao carregar os dados! Tente novamente!")
                finish()
                return@loadDataRoom
            }

            workspaceModel = WorkspaceModel.fromEntity(workspace)

            if (NetworkChangeReceiver().isNetworkConnected(this)) {
                loadUserModel(workspaceModel.creator)
            }

            taskCompleted() // Indica que a tarefa de carregar o workspace foi concluída
        }
    }

    private fun loadCitizenModel() {
        if (citizenId.isNullOrEmpty()) return

        pendingTasks++ // Iniciando a tarefa de carregar o cidadão
        citizenViewModel.loadCitizenDataRoom(citizenId!!) { citizen ->
            if (citizen == null) {
                toastMessage("Erro ao carregar os dados! Tente novamente!")
                finish()
                return@loadCitizenDataRoom
            }

            citizenModel = CitizenModel.fromEntity(citizen)
            taskCompleted() // Indica que a tarefa de carregar o cidadão foi concluída
        }
    }

    private fun taskCompleted() {
        pendingTasks-- // Diminui o contador de tarefas pendentes

        // Quando todas as tarefas forem concluídas, carrega os termos de privacidade
        if (pendingTasks == 0) {
            loadPrivacyTerm()
        }
    }

    private fun loadPrivacyTerm() {
        if (citizenId != null) {
            createCitizenPrivacyTerms()
        } else if (workspaceId != null) {
            createWorkspacePrivacyTerms()
        } else {
            createUserPrivacyTerms()
        }
    }

    private fun toastMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun createUserPrivacyTerms() {
        val privacyTerm = """
            <br>
            <h1>Política de Privacidade</h1>
            <br>
            <p>A presente Política de Privacidade regula a maneira como <strong>${supportName}</strong> coleta, utiliza, mantém e divulga as informações coletadas dos usuários (cada um, um "Usuário") do aplicativo <strong>ADECTA</strong>. Esta política de privacidade aplica-se ao aplicativo e a todos os serviços oferecidos por <strong>${supportName}</strong>.</p>
            <br>
            <h2>1. Informações Pessoais Coletadas</h2>
            <br>
            <p>Coletamos informações pessoais dos usuários de várias maneiras, incluindo, mas não se limitando a, quando os usuários se registram no aplicativo. As informações pessoais que coletamos podem incluir:</p>
            <br>
            <ul>
                <li> Nome completo</li>
                <li> Telefone</li>
                <li> Sexo</li>
                <li> CPF</li>
                <li> Número do SUS (opcional)</li>
                <li> Número de Registro</li>
                <li> Data de Nascimento</li>
                <li> Nome do Pai</li>
                <li> Nome da Mãe</li>
                <li> Local de Nascimento</li>
                <li> CEP</li>
                <li> Estado</li>
                <li> Cidade</li>
                <li> Bairro</li>
                <li> Rua</li>
                <li> Número da Casa</li>
                <li> Complemento (opcional)</li>
                <li> Status de Atividade</li>
            </ul>
            <br>
            <h2>2. Uso das Informações Coletadas</h2>
            <br>
            <p>As informações pessoais dos usuários são coletadas e utilizadas para as seguintes finalidades:</p>
            <ul>
                <li><strong>Cadastro e Identificação:</strong> Para registrar e identificar os cidadãos cadastrados no aplicativo.</li>
                <li><strong>Comunicação:</strong> Para entrar em contato com os usuários, quando necessário, e fornecer informações relacionadas ao uso do aplicativo.</li>
                <li><strong>Segurança e Autenticação:</strong> Para garantir que apenas usuários autorizados acessem os dados, utilizando medidas de segurança como criptografia e autenticação.</li>
                <li><strong>Manutenção de Registros:</strong> Para manter registros precisos e atualizados dos cidadãos cadastrados.</li>
            </ul>
            <br>
            <h2>3. Armazenamento e Proteção das Informações</h2>
            <br>
            <p>As informações coletadas são armazenadas de forma segura, utilizando criptografia tanto para o armazenamento local no dispositivo do usuário quanto para o armazenamento online no Firebase. Implementamos medidas de segurança adequadas para proteger os dados contra acesso não autorizado, alteração, divulgação ou destruição de suas informações pessoais.</p>
            <br>
            <h2>4. Compartilhamento de Informações Pessoais</h2>
            <br>
            <p>Não vendemos, trocamos ou alugamos as informações de identificação pessoal dos usuários para terceiros. Podemos compartilhar informações genéricas demográficas agregadas, não vinculadas a nenhuma informação de identificação pessoal, com nossos parceiros comerciais, afiliados de confiança e anunciantes para os fins descritos acima.</p>
            <br>
            <h2>5. Direitos dos Usuários</h2>
            <br>
            <p>Os usuários têm o direito de acessar, corrigir ou excluir suas informações pessoais. Se você deseja revisar, atualizar ou excluir suas informações pessoais, entre em contato conosco através do email <strong>$supportEmail</strong>. Analisaremos sua solicitação e tomaremos as medidas necessárias para atender sua solicitação, respeitando as obrigações legais.</p>
            <br>
            <h2>6. Alterações nesta Política de Privacidade</h2>
            <br>
            <p><strong>${supportName}</strong> tem o direito de atualizar esta política de privacidade a qualquer momento. Quando o fizermos, publicaremos uma notificação no aplicativo e revisaremos a data de atualização no topo desta página. Encorajamos os usuários a verificarem frequentemente esta página para quaisquer alterações, para que estejam informados sobre como estamos ajudando a proteger as informações pessoais que coletamos.</p>
            <br>
            <h2>7. Aceitação destes Termos</h2>
            <br>
            <p>Ao utilizar este aplicativo, você declara sua aceitação desta política de privacidade. Se você não concorda com esta política, por favor, não use o aplicativo. Seu uso continuado do aplicativo após a publicação de alterações nesta política será considerado como sua aceitação dessas alterações.</p>
            <br>
            <h2>8. Contato</h2>
            <br>
            <p>Se você tiver qualquer dúvida sobre esta Política de Privacidade, as práticas deste aplicativo ou suas interações conosco, por favor, entre em contato conosco através de:</p>
            <p><strong>${supportName}</strong><br>
            Email: <strong>${supportEmail}</strong></p>
            
            <br><hr><br>
            <h1>Termos de Serviço</h1>
            <br>
            <p>Bem-vindo ao aplicativo desenvolvido por <strong>${supportName}</strong>. Ao utilizar este aplicativo, você concorda com os seguintes Termos de Serviço. Caso não concorde com estes termos, recomendamos que não utilize o aplicativo.</p>
            <br>
            <h2>1. Uso do Aplicativo</h2>
            <br>
            <p>Ao utilizar o nosso aplicativo, o usuário concorda em fazer um uso responsável e cauteloso, respeitando as funcionalidades oferecidas. O uso inadequado ou que contrarie as finalidades previstas pode resultar na suspensão ou encerramento do seu acesso.</p>
            <br>
            <h2>2. Cadastro de Informações</h2>
            <br>
            <p>Ao se cadastrar ou ao cadastrar terceiros no aplicativo, você garante que tem a devida autorização para fornecer as informações pessoais. O fornecimento de dados sem o consentimento do titular pode violar leis de proteção de dados e resultar em penalidades previstas na legislação.</p>
            <p>O usuário é responsável pela veracidade das informações fornecidas e compromete-se a manter os dados atualizados. O uso de informações falsas ou desatualizadas pode comprometer a funcionalidade do aplicativo.</p>
            <br>
            <h2>3. Responsabilidade pelo Uso</h2>
            <br>
            <p>O usuário é integralmente responsável pelo uso que faz do aplicativo e das informações fornecidas. A <strong>${supportName}</strong> não se responsabiliza por danos ou prejuízos resultantes de uso inadequado ou indevido da plataforma.</p>
            <br>
            <h2>4. Limitações de Uso</h2>
            <br>
            <p>É estritamente proibido usar o aplicativo para atividades ilegais, ofensivas, ou que infrinjam os direitos de terceiros. Isso inclui, mas não se limita a: compartilhar dados sem autorização, acessar informações de forma não autorizada, e uso de linguagem imprópria.</p>
            <br>
            <h2>5. Alterações nos Termos</h2>
            <br>
            <p>A <strong>${supportName}</strong> se reserva o direito de modificar ou atualizar estes Termos de Serviço a qualquer momento, sem aviso prévio. Recomendamos que os usuários revisem esta página periodicamente para se manterem informados sobre possíveis mudanças. O uso contínuo do aplicativo após as alterações constitui a aceitação dos novos termos.</p>
            <br>
            <h2>6. Suporte e Contato</h2>
            <br>
            <p>Para dúvidas, suporte ou mais informações, entre em contato conosco através do email: <strong>${supportEmail}</strong>.</p>
            <br><br>
            """.trimIndent()
        binding.textPrivacyTerms.text = Html.fromHtml(privacyTerm, Html.FROM_HTML_MODE_COMPACT)
    }

    private fun createWorkspacePrivacyTerms() {
        val textResponseCreator = if (NetworkChangeReceiver().isNetworkConnected(this)) """
           <b>Responsabilidade do Criador:</b> O criador do grupo, ${userModel.username}, é responsável pela manutenção e conformidade com o termo de privacidade específico do grupo.<br><br>
    """.trimIndent() else ""

        if (workspaceId != null) {
            val privacyTerm = """
            <br><h1><b>Termo de Privacidade do Grupo</b></h1><br><br>
            Ao participar do grupo ${workspaceModel.name}, você concorda com os seguintes termos de privacidade:<br><br>
            <b>Uso dos Dados:</b> Os dados coletados no grupo serão compartilhados entre os membros do grupo, serão usados para armazenamento de informação sobre os cidadãos de um determinado bairro referenciado pelo grupo. <br><br>
            $textResponseCreator
            <b>Responsabilidade dos membros:</b> Os membros desse grupo são responsaveis pelo sigilo das informações cadastradas nesse espaço. Esses dados não deverão ser compartilhados com terceiros não autorizados. Os integrantes desse grupo serão responsabilizados por qualquer vazamento indevido dos dados aqui registrados. <br><br> 
            <b>Contato:</b> Para questões relacionadas ao termo de privacidade ou para solicitar alterações, entre em contato com o criador do grupo através do email: ${userModel.email}. 
            Para questões gerais, entre em contato com o suporte do aplicativo pelo email: <strong>${supportEmail}</strong>.<br><br>
            <b>Revisões e Atualizações:</b> O termo de privacidade pode ser revisado e atualizado periodicamente. Os membros do grupo serão informados sobre quaisquer mudanças.
        """.trimIndent()
            binding.textPrivacyTerms.text = Html.fromHtml(privacyTerm, Html.FROM_HTML_MODE_COMPACT)
        }
    }

    private fun createCitizenPrivacyTerms() {
        val privacyTerm = """
            <br><h1><b>Termo de Privacidade do Cidadão</b></h1><br><br>
            <p>Olá <strong>${citizenModel.name}</strong>,</p><br><br>
            <p>Você foi recentemente cadastrado no aplicativo <strong>ADECTA</strong>, um software da <strong>Associação de Desenvolvimento Comunitário de Tapera</strong> que registramos os dados dos cidadãos de um determinado bairro. 
            <br><br>
            Você foi cadastrado no grupo <strong>${workspaceModel.name}</strong>. Abaixo estão os detalhes do seu cadastro:</p>
            <br><br>
            <ul>
              <li><strong>Nome Completo:</strong> ${citizenModel.name}</li>
              <li><strong>Telefone:</strong> ${citizenModel.telephone}</li>
              <li><strong>CPF:</strong> ${citizenModel.cpf}</li>
              <li><strong>Número de Registro:</strong> ${citizenModel.numberregister}</li>
              <li><strong>Data de Nascimento:</strong> ${citizenModel.birthdate}</li>
              <li><strong>Nome do Pai:</strong> ${citizenModel.fathername}</li>
              <li><strong>Nome da Mãe:</strong> ${citizenModel.mothername}</li>
              <li><strong>Local de Nascimento:</strong> ${citizenModel.birthplace}</li>
            </ul>
            <br>
            <p><strong>Endereço:</strong></p>
            <ul>
              <li><strong>CEP:</strong> ${citizenModel.cep}</li>
              <li><strong>Estado:</strong> ${citizenModel.state}</li>
              <li><strong>Cidade:</strong> ${citizenModel.city}</li>
              <li><strong>Bairro:</strong> ${citizenModel.neighborhood}</li>
              <li><strong>Rua:</strong> ${citizenModel.street}</li>
              <li><strong>Número da Casa:</strong> ${citizenModel.numberhouse}</li>
              <li><strong>Complemento:</strong> ${citizenModel.addons ?: "N/A"}</li>
            </ul>
            <br>
            <p><strong>Informações do Cadastro:</strong></p>
            <ul>
              <li><strong>Criado por:</strong> ${userModel.fullname}</li>
              <li><strong>Grupo:</strong> ${workspaceModel.name}</li>
            </ul>
            <br><br>
            <p>Se você identificar qualquer erro ou desejar fazer alterações nos seus dados, por favor, entre em contato com o suporte através do email 
            <strong>${supportEmail}</strong> ou com o criador do grupo pelo email <strong>${userModel.email}</strong>.</p>
            <br><br>
            <p>Agradecemos a sua atenção e colaboração.</p>
            <p>Atenciosamente, 
            <strong>ADECTA</strong><br>
            
            <br><br><p>Desenvolvido por <strong>${supportName}</strong><p><br>
            <p>Email: <strong>${supportEmail}</strong></p><br>
           """.trimIndent()
        binding.textPrivacyTerms.text = Html.fromHtml(privacyTerm, Html.FROM_HTML_MODE_COMPACT)
    }
}
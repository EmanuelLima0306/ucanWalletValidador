/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package servlets;

import bean.ContaBean;
import bean.TransacaoBean;
import com.google.gson.Gson;
import enumerator.TipoMensagem;
import incriptacao.RSAEncryption;
import incriptacao.RSAKeyUtils;
import java.io.IOException;
import java.security.PrivateKey;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.ContaModel;
import model.TransacaoModel;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import util.TransacaoServer;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author emanuellima
 */
@WebServlet(name = "IndexServlet", urlPatterns = {"/IndexServlet", "/", "/index"})
public class IndexServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect("index.jsp");
    }

    private ConnectionFactory connectionFactory;
    private Destination filaValidador;
    MessageConsumer consumer;

    @Override
    public void init() throws ServletException {
        try {
//             Inicializar a ConnectionFactory com a configuração adequada
            connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

            // Definir a fila de validação de transações
            filaValidador = new ActiveMQQueue("fila.validacao");

            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageConsumer consumer = session.createConsumer(filaValidador);

            consumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            String mensagem = textMessage.getText();

                            System.out.println("Transação recebida::: " + mensagem);
                            // Converter a mensagem em uma transação
                            Gson gson = new Gson();
                            TransacaoServer transacaoServer = gson.fromJson(mensagem, TransacaoServer.class);
//
//                            // Realizar validação da transação
                            TransacaoServer TransacaoServerResposta = validarTransacao(transacaoServer);

                            // Enviar a resposta para a fila de retorno especificada pelo cliente
                            connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");

                            // Definir a fila de validação de transações
                            Destination filaRetorno = new ActiveMQQueue("fila.resposta");

                            Connection connectionResposta = connectionFactory.createConnection();
                            connectionResposta.start();
                            Session sessionRespost = connectionResposta.createSession(false, Session.AUTO_ACKNOWLEDGE);
                            MessageProducer producer = sessionRespost.createProducer(filaRetorno);
                            TextMessage resposta = sessionRespost.createTextMessage();
                            resposta.setJMSReplyTo(filaRetorno);
                            resposta.setText(TransacaoServerResposta.toJson());
                            producer.send(resposta);

//                            producer.close();
//                            connectionResposta.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            connection.start();
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {

        }

    }

    private TransacaoServer validarTransacao(TransacaoServer transacaoServer) {
        //Recuperação da conta de origem
        ContaBean contaBean = new ContaBean();
        contaBean.findById(transacaoServer.getPkContaOrigem().intValue());
        ContaModel contaOrigem = contaBean.getModel();

        try {
            //Recuperação da chave privada
            PrivateKey privateKey = RSAKeyUtils.privateKeyFromBytes(contaOrigem.getChavePrivada());

            int numeroContaDestino = (int) RSAEncryption.decrypt(transacaoServer.getNumeroContaDestino(), privateKey);
            double valor = (double) RSAEncryption.decrypt(transacaoServer.getValor(), privateKey);
            Timestamp dataTransacao = (Timestamp) RSAEncryption.decrypt(transacaoServer.getDataTransacao(), privateKey);

            TransacaoBean transacaoBean = new TransacaoBean();
            //Criar instancia de ContaBean

            // Criar a instacia da TransacaoBean
            TransacaoModel transacaoModel = new TransacaoModel();

            contaBean.findByNumeroConta(numeroContaDestino);
            ContaModel contaDestino = contaBean.getModel();
            //Passagem de conta para transacao
            transacaoModel.setContaOrigem(contaOrigem);
            transacaoModel.setContaDestino(contaDestino);
            transacaoModel.setValor(valor);
            transacaoModel.setDataTransacao(dataTransacao);
            transacaoModel.setDataValidacao(new Timestamp(new Date().getTime()));

            transacaoBean.setModel(transacaoModel);
            //
            TipoMensagem tipoMensagem = transacaoBean.saveOrUpdate();
            System.out.println("Servidor recebeu:::"+ tipoMensagem.getDescricao());
            transacaoServer.setTipoMensagem(RSAEncryption.encrypt(tipoMensagem, RSAKeyUtils.publicKeyFromBytes(contaOrigem.getChavePublica())));
        } catch (Exception ex) {
            System.err.println(ex);
            try {
                transacaoServer.setTipoMensagem(RSAEncryption.encrypt(TipoMensagem.ERRO, RSAKeyUtils.publicKeyFromBytes(contaOrigem.getChavePublica())));
            } catch (Exception ex1) {
                Logger.getLogger(IndexServlet.class.getName()).log(Level.SEVERE, null, ex1);
            }

        }
        return transacaoServer;
    }

}

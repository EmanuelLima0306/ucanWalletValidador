/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bean;

import dao.TransacaoDao;
import enumerator.Estado;
import enumerator.TipoMensagem;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.TransacaoModel;

/**
 *
 * @author emanuellima
 */
public class TransacaoBean {

    private TransacaoModel transacaoModel;
    private TransacaoDao transacaoDao;

    public TipoMensagem saveOrUpdate() {
        try {
            if (transacaoModel != null) {
                if (!transacaoModel.isEmpty()) {
                    TipoMensagem tipoMensagem;
                    ContaBean contaBean = new ContaBean();
                    transacaoDao = new TransacaoDao();
                    //Verifica se a conta de origigem é diferente que a de destino
                    if (transacaoModel.getContaOrigem().getPkConta().intValue() != transacaoModel.getContaDestino().getPkConta().intValue()) {

                        tipoMensagem = contaBean.saldoIsDisponivel(transacaoModel.getContaOrigem(), transacaoModel.getValor()); // verifica se na conta de origem tem saldo suficiente para transferir
                        //Verifica se a conta de origem tem saldo suficiente
                        if (tipoMensagem == TipoMensagem.SUCESSO) {
                            //Verifica o estado da conta de origem
                            if (transacaoModel.getContaOrigem().getEstado() == Estado.BLOQUEADA) {
                                transacaoModel.setEstado(Estado.REJEITADA);

                            } else {
                                transacaoModel.setEstado(Estado.VALIDA);
                            }

                        } else {
                            transacaoModel.setEstado(Estado.REJEITADA);
                        }
                    } else {
                        transacaoModel.setEstado(Estado.REJEITADA);
                        tipoMensagem = TipoMensagem.ERRO_CONTA_ORIGEM_IGUAL_DESTINO;
                    }

                    //faz a inserção no banco
                    if (transacaoDao.insert(transacaoModel)) {

                        //verifica o estado da transacao se é validada
                        if (transacaoModel.getEstado() == Estado.VALIDA) {
                            contaBean.reduzirContabilistico(transacaoModel.getContaOrigem(), transacaoModel.getValor());
                            contaBean.aumentarContabilisticoDisponivel(transacaoModel.getContaDestino(), transacaoModel.getValor());
                            tipoMensagem = TipoMensagem.SUCESSO;
                        } else {
                            //devolver o valor ao saldo disponivel do cliente de origem
                            contaBean.devolverDisponivel(transacaoModel.getContaOrigem(), transacaoModel.getValor());
                        }
                        return tipoMensagem;
                    }
                    return TipoMensagem.ERRO_INESPERADO;

                } else {
                    return TipoMensagem.PREENCHA_CAMPO;
                }
            } else {
                return TipoMensagem.NULL;
            }

        } catch (Exception ex) {
            System.err.print(ex);
            return TipoMensagem.ERRO_INESPERADO;
        }

    }

    public List<TransacaoModel> getAll() {
        try {
            transacaoDao = new TransacaoDao();
            return transacaoDao.getAll();

        } catch (Exception ex) {
            Logger.getLogger(TransacaoBean.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public TransacaoModel getLast() {
        try {
            transacaoDao = new TransacaoDao();
            return transacaoDao.findLast();

        } catch (Exception ex) {
            Logger.getLogger(TransacaoBean.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public TransacaoModel getModel() {
        return this.transacaoModel;
    }

    public void setModel(TransacaoModel transacaoModel) {
        this.transacaoModel = transacaoModel;
    }
}

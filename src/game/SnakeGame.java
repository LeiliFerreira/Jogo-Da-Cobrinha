package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.Random;

public class SnakeGame extends JFrame {

    private static final int LARGURA_TELA = 600;
    private static final int ALTURA_TELA = 500;
    private static final int TAMANHO_BLOCO = 20;
    private static final int UNIDADES = LARGURA_TELA * ALTURA_TELA / (TAMANHO_BLOCO * TAMANHO_BLOCO);
    private static final int INTERVALO = 200;
    private static final String NOME_FONTE = "Ink Free";
    private final int[] eixoX = new int[UNIDADES];
    private final int[] eixoY = new int[UNIDADES];
    private int corpoCobra = 6;
    private int blocosComidos;
    private int blocoX;
    private int blocoY;
    private char direcao = 'D'; 
    private boolean estaRodando = false;
    private Timer timer;
    private Random random;

    private CardLayout cardLayout;
    private JPanel cardPanel;

    private Connection conexao;
    private Statement statement;
    private int pontuacaoMaisAlta;

    SnakeGame() {
        random = new Random();
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        TelaInicial telaInicial = new TelaInicial();
        cardPanel.add(telaInicial, "telaInicial");
        setTitle("Snake Game");

        TelaJogo telaJogo = new TelaJogo();
        cardPanel.add(telaJogo, "telaJogo");

        setPreferredSize(new Dimension(LARGURA_TELA, ALTURA_TELA));
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        timer = new Timer(INTERVALO, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (estaRodando) {
                    andar();
                    alcancarBloco();
                    validarLimites();
                    repaint();
                }
            }
        });

        cardLayout.show(cardPanel, "telaInicial");
        add(cardPanel);
        setFocusable(true);
        addKeyListener(new LeitorDeTeclasAdapter());

        conectarBancoDados();
    }

    private void conectarBancoDados() {
        try {
            Class.forName("org.sqlite.JDBC");
            conexao = DriverManager.getConnection("jdbc:sqlite:pontuacoes.db");
            statement = conexao.createStatement();

            // Crie a tabela se não existir
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS pontuacoes (pontuacao INTEGER)");

            // Obtenha a pontuação mais alta
            ResultSet resultSet = statement.executeQuery("SELECT MAX(pontuacao) FROM pontuacoes");
            if (resultSet.next()) {
                pontuacaoMaisAlta = resultSet.getInt(1);
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void fecharConexao() {
        try {
            if (statement != null) {
                statement.close();
            }
            if (conexao != null) {
                conexao.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void iniciarJogo() {
        criarBloco();
        estaRodando = true;
        timer.start();
        cardLayout.show(cardPanel, "telaJogo");

        // Exibir um alerta com a pontuação mais alta
        JOptionPane.showMessageDialog(this, "Pontuação mais alta atingida: " + pontuacaoMaisAlta, "Pontuação mais alta", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void paint(Graphics g) {
        Image bufferImage = createImage(getWidth(), getHeight());
        Graphics bufferGraphics = bufferImage.getGraphics();
        desenharTela(bufferGraphics);
        g.drawImage(bufferImage, 0, 0, this);
    }

    public void desenharTela(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, LARGURA_TELA, ALTURA_TELA);

        if (estaRodando) {
            g.setColor(Color.RED);
            g.fillOval(blocoX, blocoY, TAMANHO_BLOCO, TAMANHO_BLOCO);

            for (int i = 0; i < corpoCobra; i++) {
                if (i == 0) {
                    g.setColor(Color.GREEN);
                    g.fillRect(eixoX[0], eixoY[0], TAMANHO_BLOCO, TAMANHO_BLOCO);
                } else {
                    g.setColor(new Color(45, 180, 0));
                    g.fillRect(eixoX[i], eixoY[i], TAMANHO_BLOCO, TAMANHO_BLOCO);
                }
            }
            g.setColor(Color.RED);
            g.setFont(new Font(NOME_FONTE, Font.BOLD, 40));
            FontMetrics metrics = getFontMetrics(g.getFont());
            g.drawString("Pontos: " + blocosComidos, (LARGURA_TELA - metrics.stringWidth("Pontos: " + blocosComidos)) / 2, ALTURA_TELA / 2 - 150);
        } else {
            fimDeJogo(g);
        }
    }

    private void criarBloco() {
        blocoX = random.nextInt(LARGURA_TELA / TAMANHO_BLOCO) * TAMANHO_BLOCO;
        blocoY = random.nextInt(ALTURA_TELA / TAMANHO_BLOCO) * TAMANHO_BLOCO;
    }

    private void fimDeJogo(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font(NOME_FONTE, Font.BOLD, 40));
        FontMetrics fontePontuacao = getFontMetrics(g.getFont());
        g.drawString("Pontos: " + blocosComidos, (LARGURA_TELA - fontePontuacao.stringWidth("Pontos: " + blocosComidos)) / 2, ALTURA_TELA / 2 + 50);
        g.setColor(Color.RED);
        g.setFont(new Font(NOME_FONTE, Font.BOLD, 75));
        FontMetrics fonteFinal = getFontMetrics(g.getFont());
        g.drawString("GAME OVER", (LARGURA_TELA - fonteFinal.stringWidth("Fim do Jogo")) / 2-30, ALTURA_TELA / 2);

        // Salvar a pontuação ao final do jogo
        salvarPontuacao();
    }

    private void andar() {
        for (int i = corpoCobra; i > 0; i--) {
            eixoX[i] = eixoX[i - 1];
            eixoY[i] = eixoY[i - 1];
        }

        switch (direcao) {
            case 'C':
                eixoY[0] = eixoY[0] - TAMANHO_BLOCO;
                break;
            case 'B':
                eixoY[0] = eixoY[0] + TAMANHO_BLOCO;
                break;
            case 'E':
                eixoX[0] = eixoX[0] - TAMANHO_BLOCO;
                break;
            case 'D':
                eixoX[0] = eixoX[0] + TAMANHO_BLOCO;
                break;
            default:
                break;
        }
    }

    private void alcancarBloco() {
        if (eixoX[0] == blocoX && eixoY[0] == blocoY) {
            corpoCobra++;
            blocosComidos++;
            criarBloco();
        }
    }

    private void validarLimites() {
        for (int i = corpoCobra; i > 0; i--) {
            if (eixoX[0] == eixoX[i] && eixoY[0] == eixoY[i]) {
                estaRodando = false;
                break;
            }
        }

        if (eixoX[0] < 0 || eixoX[0] >= LARGURA_TELA) {
            estaRodando = false;
        }

        if (eixoY[0] < 0 || eixoY[0] >= ALTURA_TELA) {
            estaRodando = false;
        }

        if (!estaRodando) {
            timer.stop();
        }
    }

    private void salvarPontuacao() {
        try {
            statement.executeUpdate("INSERT INTO pontuacoes (pontuacao) VALUES (" + blocosComidos + ")");

            if (blocosComidos > pontuacaoMaisAlta) {
                pontuacaoMaisAlta = blocosComidos;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public class LeitorDeTeclasAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (direcao != 'D') {
                        direcao = 'E';
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (direcao != 'E') {
                        direcao = 'D';
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (direcao != 'B') {
                        direcao = 'C';
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (direcao != 'C') {
                        direcao = 'B';
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public class TelaInicial extends JPanel {

        public TelaInicial() {
            setLayout(new BorderLayout());

            JButton btnIniciarJogo = new JButton("TOQUE NA TELA PARA JOGAR");
            btnIniciarJogo.setBackground(new Color(211, 211, 211));
            btnIniciarJogo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    iniciarJogo();
                }
            });

            add(btnIniciarJogo, BorderLayout.CENTER);
        }
    }

    public class TelaJogo extends JPanel {
        
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SnakeGame snakeGame = new SnakeGame();
                snakeGame.setVisible(true);
                snakeGame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        snakeGame.fecharConexao();
                        System.exit(0);
                    }
                });
            }
        });
    }
}

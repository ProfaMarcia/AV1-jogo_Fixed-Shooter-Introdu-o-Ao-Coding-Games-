import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class FixedShooterGame extends JPanel implements ActionListener, KeyListener {

    // Tela
    public static final int WIDTH = 400;
    public static final int HEIGHT = 600;

    // Timer para game loop
    private Timer timer;

    // Jogador
    private Player player;

    // Listas de tiros e inimigos
    private ArrayList<Bullet> bullets = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<EnemyBullet> enemyBullets = new ArrayList<>();

    // Estado
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean spacePressed = false;

    private int score = 0;
    private boolean gameOver = false;

    public FixedShooterGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        player = new Player(WIDTH / 2 - 20, HEIGHT - 80);

        createEnemies();

        timer = new Timer(16, this); // ~60fps
        timer.start();
    }

    private void createEnemies() {
        enemies.clear();
        int rows = 4;
        int cols = 6;
        int startX = 80;
        int startY = 60;
        int spacingX = 70;
        int spacingY = 60;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                enemies.add(new Enemy(startX + c * spacingX, startY + r * spacingY));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Desenha jogador
        player.draw(g);

        // Desenha inimigos
        for (Enemy e : enemies) {
            e.draw(g);
        }

        // Desenha tiros
        for (Bullet b : bullets) {
            b.draw(g);
        }

        for (EnemyBullet eb : enemyBullets) {
            eb.draw(g);
        }

        // Placar e vidas
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Pontos: " + score, 10, 20);
        g.drawString("Vidas: " + player.lives, WIDTH - 100, 20);

        // Game over
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.setColor(Color.RED);
            g.drawString("GAME OVER", WIDTH / 2 - 150, HEIGHT / 2);
        }
    }

    // Atualiza estado do jogo a cada frame (~60fps)
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) {
            timer.stop();
            return;
        }

        // Movimento do jogador
        if (leftPressed) {
            player.move(-5);
        } else if (rightPressed) {
            player.move(5);
        }

        // Atirar
        if (spacePressed && player.canShoot()) {
            bullets.add(new Bullet(player.x + player.width / 2, player.y));
            player.resetShootCooldown();
        }
        player.updateShootCooldown();

        // Atualiza tiros do jogador
        Iterator<Bullet> itB = bullets.iterator();
        while (itB.hasNext()) {
            Bullet b = itB.next();
            b.update();
            if (b.y < 0) {
                itB.remove();
            }
        }

        // Atualiza tiros inimigos
        Iterator<EnemyBullet> itEB = enemyBullets.iterator();
        while (itEB.hasNext()) {
            EnemyBullet eb = itEB.next();
            eb.update();

            if (eb.getRect().intersects(player.getRect())) {
                player.lives--;
                itEB.remove();
                if (player.lives <= 0) {
                    gameOver = true;
                }
            } else if (eb.y > HEIGHT) {
                itEB.remove();
            }
        }

        // Atualiza inimigos
        Random rand = new Random();
        Iterator<Enemy> itE = enemies.iterator();
        while (itE.hasNext()) {
            Enemy enemy = itE.next();
            enemy.update();

            // Inimigos atiram aleatoriamente
            if (rand.nextDouble() < 0.002) {
                enemyBullets.add(new EnemyBullet(enemy.x + enemy.width / 2, enemy.y + enemy.height));
            }

            // Colisão inimigo com jogador
            if (enemy.getRect().intersects(player.getRect())) {
                player.lives--;
                itE.remove();
                if (player.lives <= 0) {
                    gameOver = true;
                }
                continue;
            }

            // Colisão com tiros do jogador
            boolean enemyHit = false;
            Iterator<Bullet> itBullets = bullets.iterator();
            while (itBullets.hasNext()) {
                Bullet b = itBullets.next();
                if (enemy.getRect().intersects(b.getRect())) {
                    score += 10;
                    enemyHit = true;
                    itBullets.remove();
                    break;
                }
            }

            if (enemyHit) {
                itE.remove();
            }
        }

        // Se todos os inimigos foram derrotados, cria nova onda
        if (enemies.isEmpty()) {
            createEnemies();
        }

        repaint();
    }

    // Controle de teclado
    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) return;

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = true;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            spacePressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = false;
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            spacePressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Classes do jogo

    static class Player {
        int x, y, width = 40, height = 20;
        int lives = 3;

        private int shootCooldown = 0; // controle de tiro

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void move(int dx) {
            x += dx;
            if (x < 0) x = 0;
            if (x > WIDTH - width) x = WIDTH - width;
        }

        public void draw(Graphics g) {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, width, height);
            // base do canhão (laser)
            g.fillRect(x + width / 2 - 5, y - 10, 10, 10);
        }

        public Rectangle getRect() {
            return new Rectangle(x, y, width, height);
        }

        public boolean canShoot() {
            return shootCooldown == 0;
        }

        public void resetShootCooldown() {
            shootCooldown = 15; // frames de cooldown (~0.25 seg)
        }

        public void updateShootCooldown() {
            if (shootCooldown > 0) shootCooldown--;
        }

        public void loseLife() {
            lives--;
        }
    }

    static class Enemy {
        int x, y, width = 40, height = 30;
        int speedX = 2;
        boolean movingRight = true;

        public Enemy(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            if (movingRight) {
                x += speedX;
                if (x > WIDTH - width) {
                    movingRight = false;
                    y += 20;
                }
            } else {
                x -= speedX;
                if (x < 0) {
                    movingRight = true;
                    y += 20;
                }
            }
        }

        public void draw(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(x, y, width, height);
            // olhos
            g.setColor(Color.WHITE);
            g.fillOval(x + 10, y + 8, 8, 8);
            g.fillOval(x + 22, y + 8, 8, 8);
        }

        public Rectangle getRect() {
            return new Rectangle(x, y, width, height);
        }
    }

    static class Bullet {
        int x, y;
        int width = 5, height = 10;
        int speed = 10;

        public Bullet(int x, int y) {
            this.x = x - width / 2;
            this.y = y;
        }

        public void update() {
            y -= speed;
        }

        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, width, height);
        }

        public Rectangle getRect() {
            return new Rectangle(x, y, width, height);
        }
    }

    static class EnemyBullet {
        int x, y;
        int width = 5, height = 10;
        int speed = 5;

        public EnemyBullet(int x, int y) {
            this.x = x - width / 2;
            this.y = y;
        }

        public void update() {
            y += speed;
        }

        public void draw(Graphics g) {
            g.setColor(Color.PINK);
            g.fillRect(x, y, width, height);
        }

        public Rectangle getRect() {
            return new Rectangle(x, y, width, height);
        }
    }

    // Main para rodar o jogo
    public static void main(String[] args) {
        JFrame frame = new JFrame("Fixed Shooter");
        FixedShooterGame gamePanel = new FixedShooterGame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

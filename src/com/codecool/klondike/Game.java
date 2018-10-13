package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.util.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 1;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 30;

    boolean isGameWon = false;
    private boolean cheat = false;
    ScheduledExecutorService service;
    int playTime = 0;



    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private void createRestartButton() {
        Image image = new Image(getClass().getResourceAsStream("/table/res.png"));

        Button rest = new Button();
        rest.setGraphic(new ImageView(image));
        
        rest.setLayoutX(10);
        rest.setLayoutY(60);
        getChildren().add(rest);;
        rest.setOnAction(this::handleButtonAction);

        
    }

    private void createCheatButton() {
        
        Image image = new Image(getClass().getResourceAsStream("/table/cheat.png"));
        Button cheat = new Button();
        cheat.setGraphic(new ImageView(image));
        cheat.setLayoutX(10);
        cheat.setLayoutY(100);
        getChildren().add(cheat);;
        cheat.setOnAction(this::handleButtonCheatAction);


    }

    private void handleButtonAction(ActionEvent event) {
        clearAllPiles();
        deck = Card.createNewDeck();
        initPiles();
        dealCards();
        createRestartButton();
        createCheatButton();      
    }

    private void handleButtonCheatAction(ActionEvent event) {
        if (cheat == false) {
            this.cheat = true;
        }
        else {
            this.cheat = false;
        }
        
        
    }

    private void clearAllPiles() {
        getChildren().clear();
        playTime = 0;
        for (int i = 0; i < 4; i++) {
            foundationPiles.get(i).clear();
        }
        for (int i = 0; i < 7; i++) {
            tableauPiles.get(i).clear();
        }
    }

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        if (stockPile.isEmpty()) {
           refillStockFromDiscard();
       }
   };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        flipLastCardsInTableauPiles();
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        draggedCards.add(card);        

        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        card.toFront();
        card.setTranslateX(offsetX);
        card.setTranslateY(offsetY);

        if (activePile.getPileType() == Pile.PileType.TABLEAU) {
            ObservableList<Card> activePileList = activePile.getCards();
            int indexOfDraggedCard = activePileList.indexOf(card);
            for (int i = indexOfDraggedCard + 1; i < activePileList.size(); i++) {
                draggedCards.add(activePileList.get(i));

                activePileList.get(i).getDropShadow().setRadius(20);
                activePileList.get(i).getDropShadow().setOffsetX(10);
                activePileList.get(i).getDropShadow().setOffsetY(10);

                activePileList.get(i).toFront();
                activePileList.get(i).setTranslateX(offsetX);
                activePileList.get(i).setTranslateY(offsetY);
            }              
        }        
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        Pile pile2 = getValidIntersectingPile(card, foundationPiles);      
        if (pile != null) {
            handleValidMove(card, pile);
        } else if (pile2 != null) {
            handleValidMove(card, pile2);
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards.clear();
        }        
    };  

    public boolean isGameWon() {        
        
        int cardsOnFoundationPilesCounter = 0;

        for (Pile foundationPile : foundationPiles) {
            if (!foundationPile.isEmpty()) {
                for (Card card : foundationPile.getCards()) {
                    cardsOnFoundationPilesCounter++;
                }
            }
        }
        if (cardsOnFoundationPilesCounter == 52) {
            return true;
        } else {
            cardsOnFoundationPilesCounter = 0;
            return false;
        }
    }    

    public Game() {    
            
        deck = Card.createNewDeck();
        initPiles();
        dealCards();

        Runnable runnable = new Runnable() {
            public void run() {
                playTime++;
                if(isGameWon()) {
                    int playTimeMinutes = playTime / 60;
                    int playTimeSeconds = playTime % 60;
                    System.out.format("%s %d:%d", 
                            "You win the game! You have played ",
                             playTimeMinutes,
                             playTimeSeconds);
                    scatterTheCards();
                    service.shutdownNow();
                }
            }
        };          
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);        
        createRestartButton();
        createCheatButton();
    }

    public Game(int number) {
        
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        ArrayList<Card> iterDiscardPile = new ArrayList<Card>();
        for (Card sCard : discardPile.getCards()) {
            iterDiscardPile.add(sCard);
        }

        for (Card singleCard : iterDiscardPile) {
            singleCard.flip();
            singleCard.moveToPile(stockPile);
        }
        discardPile.getCards().clear();
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
            
        if (destPile.getPileType().equals(Pile.PileType.FOUNDATION)) {
            if (!cheat) {
                if (destPile.isEmpty() && card.getRank() == 1) {
                    return true;
                } else if (destPile.isEmpty() && !(card.getRank() == 1)) {
                    return false;
                } else if (!destPile.isEmpty() && (card.getRank() == 1)) {
                    return false;
                } else if (!destPile.isEmpty() && !(card.getRank() == 1)) {
                    Card destFoundationPileTopCard = destPile.getCards().get(destPile.getCards().size() - 1);
                    if (card.getRank() - 1 == ((destFoundationPileTopCard.getRank()))
                        && Card.isSameSuit(card, destFoundationPileTopCard)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                return true;
            }            
        } else if (destPile.getPileType().equals(Pile.PileType.TABLEAU)) {
            if (destPile.isEmpty() && card.getRank() == 13) {
                return true;
            } else if (destPile.isEmpty() && !(card.getRank() == 13)) {
                return false;
            } else if (!destPile.isEmpty() && !(card.getRank() == 13 && (card.getRank() == 1))) {
                Card destTableauPileTopCard = destPile.getCards().get(destPile.getCards().size() - 1);              
                if (card.isFaceDown() == false 
                        && card.getRank() + 1 == ((destTableauPileTopCard.getRank()))
                        && Card.isOppositeColor(card, destTableauPileTopCard)) {                
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(95);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(285);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(610 + i * 180);
            foundationPile.setLayoutY(20);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(95 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }
    private void flipLastCardsInTableauPiles() {
        for (Pile pile : tableauPiles) {
            if (!pile.isEmpty()) {
                int pileSize = pile.getCards().size();
                if (pile.getCards().get(pileSize - 1).isFaceDown() == true) {
                    pile.getCards().get(pileSize - 1).flip();
                }
            }            
        }
    }
    


    public void dealCards() {
        Iterator<Card> deckIterator = deck.iterator();

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j <= i; j++) {
                Card card = deckIterator.next();
                tableauPiles.get(i).addCard(card);
                addMouseEventHandlers(card);
                getChildren().add(card);
                deckIterator.remove();
            }
        }
        flipLastCardsInTableauPiles();
        deckIterator.forEachRemaining(card -> {       
            stockPile.addCard(card);
            addMouseEventHandlers(card);
            getChildren().add(card);
        });
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

}

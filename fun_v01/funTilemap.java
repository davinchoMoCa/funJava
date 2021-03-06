package fun_v01;

import fun_v01.csvUtils.CsvReader;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

/**
 * Ésta clase crea los niveles/mapas del juego.
 * Básicamente lo que hace es que carga el CSV del mapa creado en funLevelEditor
 * y convierte esa data en una serie de funTiles que 'calcan' el nivel.
 * @author Marvin Gonzalez, Kevin Gutierrez, Néstor Villalobos
 */
public abstract class funTilemap {
    
    //*******************************ATRIBUTOS*******************************//
    //***********************************************************************//
    //***********************************************************************//

    private BufferedImage imagen;
    private String imagenURL;
    private String csvURL;
    private int anchoMapa;
    private int altoMapa;
    private int anchoTile;
    private int altoTile;
    private Rectangle clip;
    private ArrayList<funTile> tiles;
    private ArrayList<BufferedImage> tilesImgs;
    
    //******************************CONSTRUCTOR******************************//
    //***********************************************************************//
    //***********************************************************************//
    
    /**
     * El constructor de funTilemap
     * Usando un URL se carga un archivo CSV (que debe contener el mapa realizado en funLevelEditor).
     * Después usando la información de éste CSV se carga funTiles temporales con 
     * la info respectiva de imagen que llevarán y el tipo de colisión que llevan.
     * Luego se carga la imagen tilemap que se usa de base.
     * Se crean los tilemaps individuales base.
     * De la carga de los tiles sobre el escenario se encargará funScreen > agregarTilemap
     * @param csvURL La dirección URL que lleva al mapa CSV
     */
    public funTilemap(String csvURL){
        this.csvURL = csvURL;
        setImagenURL("");
        setAnchoMapa(0);
        setAltoMapa(0);
        setAnchoTile(0);
        setAltoTile(0);
        setTiles(new ArrayList<funTile>());
        setTilesImgs(new ArrayList<BufferedImage>());
        cargarMapaCSV();
        crearImagen();
        crearTilemapsImgs();
        
    }
    
    //*****************************OTROS MÈTODOS*****************************//
    //***********************************************************************//
    //***********************************************************************//
    
    /**
     * Éste método carga el archivo CSV y filtra de él la siguiente información clave:
     * El URL de la imagen que se uso de base para pintar los tiles
     * El ancho del mapa
     * El alto del mapa
     * El ancho de los tiles
     * El alto de los tiles
     * Si los diferentes tiles tendrán colisión o no.
     */
    public void cargarMapaCSV(){
        try {
                String path = getClass().getResource(".").getPath() + getCsvURL(); 
                CsvReader mapa = new CsvReader(path);                 
                mapa.readHeaders();                
                mapa.readRecord();                             
                
                setImagenURL(mapa.get("URLTilemap"));
                setAnchoMapa(Integer.parseInt(mapa.get("AnchoMapa")));
                setAltoMapa(Integer.parseInt(mapa.get("AltoMapa")));
                setAnchoTile(Integer.parseInt(mapa.get("AnchoTile")));
                setAltoTile(Integer.parseInt(mapa.get("AltoTile")));              

                while (mapa.readRecord())
                {                   
                        
                        int tileId = Integer.parseInt(mapa.get("TileID"));
                        String colision = mapa.get("Colision");
                        boolean tieneColision = false;
                        if(colision.equals("true")) tieneColision = true;
                        crearTile(tileId, tieneColision);
                }

                    mapa.close();

            } catch (FileNotFoundException e) {
                    System.out.println("NOTIFICACION: No se pudo encontrar el archivo CSV: "+e);
            } catch (IOException e) {
                    System.out.println("NOTIFICACION: No se pudo cargar el archivo CSV. Verifique que el mapa haya sido creado con el funLevelEditor "+e);
            }
    }
    
    /**
     * Se crea la imagen base tilemap de la cual se obtienen los tiles
     */
    public void crearImagen(){   
        
        BufferedImage imgTemp = null;
        try {
            File fAux = new File(getCsvURL());            
            String[] pathRoot = getCsvURL().split(fAux.separator);
            String rutaLimpia = "";
            for (int i = 0; i < pathRoot.length; i++) {
                if(!pathRoot[i].contains(".")) rutaLimpia = rutaLimpia + pathRoot[i] + fAux.separator;
            }            
            String path = getClass().getResource(".").getPath() + rutaLimpia + getImagenURL();
            File f = new File(path);
            imgTemp = ImageIO.read(f);
            setImagen(imgTemp); 
        
        } catch (IOException e) {
            System.out.println("NOTIFICACION: El Tilemap '" + this + " (" + getImagenURL() +") no pudo ser cargado: " + e);
        }
    }
    
    /**
     * Crea funTiles temporales que contienen la información básica de 
     * colisión y Id de imagen que les tocará representar.
     * @param tileId El ID del funTile (que se usará para asignarle la imagen correspondiente)
     * @param colision El tipo de colisión que permitirá este funTile
     */
    public void crearTile(int tileId, boolean colision){
        funTile tile = new funTile(getAnchoTile(), getAltoTile(),0,0);
        tile.setId(tileId);
        tile.setTieneColisiones(colision);
        getTiles().add(tile);
    }
    
    /**
     * Carga el ArrayList<BufferedImage> tileImgs con todas las imagenes que representa el tilemap base
     * según los tiles que se usaron.
     * En otras palabras, es prácticamente un clon de lo que uno ve en la ventana Tilemaps del funLevelEditor,
     * pero a nivel de data unicamente.
     */
    public void crearTilemapsImgs(){                
        int i = 0;
        int yInicial = 0;

        for (int xInicial = 0; xInicial < getImagen().getWidth(); xInicial += getAnchoTile()) {
            BufferedImage imgTemp = null;
            try {
                imgTemp = cropImagen(getImagen(),getAnchoTile(),getAltoTile(),xInicial,yInicial);
            } catch (Exception e) {
                System.out.println("NOTIFICACION: No se pudo cropear la imagen");
            }
            
            getTilesImgs().add(imgTemp);
            
            if((xInicial + getAnchoTile()) >= getImagen().getWidth()) {
                yInicial += getAltoTile();
                xInicial =- getAnchoTile();
            }
            
            if((yInicial + getAltoTile()) > getImagen().getHeight()) xInicial = getImagen().getWidth()+1;
        }
        
    }
        
    /**
     * Éste método crea una imagen recortando el tilemap base según las proporciones enviadas.
     * @param img La imagen del tilemap base.
     * @param cropWidth El ancho de la imagen que se recortará.
     * @param cropHeight El alto de la imagen que se recortará.
     * @param cropStartX La posición en X desde donde se empezará a recortar relativo al tilemap base.
     * @param cropStartY La posición en Y desde donde se empezará a recortar relativo al tilemap base.
     * @return La imagen recortada.
     * @throws Exception 
     */
    public BufferedImage cropImagen(BufferedImage img, int cropWidth,
            int cropHeight, int cropStartX, int cropStartY) throws Exception {
        BufferedImage clipped = null;
        Dimension size = new Dimension(cropWidth, cropHeight);

        crearClip(img, size, cropStartX, cropStartY);

        try {
            int w = clip.width;
            int h = clip.height;

            clipped = img.getSubimage(clip.x, clip.y, w, h);
        } catch (RasterFormatException rfe) {
            System.out.println("NOTIFICACION: Raster format error: " + rfe.getMessage());
            return null;
        }
        return clipped;
    }

    /**
     * Este metodo recorta la imagen original según los parametros
     * Si el rectángulo que se forma se sale de la imagen, se ajusta automaticamente
     * Éste método usa código tomado de otra fuente
     * @param img La imagen base original
     * @param size El tamaño del rectangulo de recorte
     * @param clipX El inicio en X de la imagen recortada
     * @param clipY El inicio en Y de la imagen recortada
     * @throws Exception
     */
    private void crearClip(BufferedImage img, Dimension size,
            int clipX, int clipY) throws Exception {

        boolean isClipAreaAdjusted = false;

        if (clipX < 0) {
            clipX = 0;
            isClipAreaAdjusted = true;
        }

        if (clipY < 0) {
            clipY = 0;
            isClipAreaAdjusted = true;
        }

        if ((size.width + clipX) <= img.getWidth()
                && (size.height + clipY) <= img.getHeight()) {

            clip = new Rectangle(size);
            clip.x = clipX;
            clip.y = clipY;
        } else {


            if ((size.width + clipX) > img.getWidth()) {
                size.width = img.getWidth() - clipX;
            }


            if ((size.height + clipY) > img.getHeight()) {
                size.height = img.getHeight() - clipY;
            }

            clip = new Rectangle(size);
            clip.x = clipX;
            clip.y = clipY;

            isClipAreaAdjusted = true;

        }
    }
    
    //******************************GETS-&-SETS******************************//
    //***********************************************************************//
    //***********************************************************************//
  

    /**
     * @return the imagen
     */
    public BufferedImage getImagen() {
        return imagen;
    }

    /**
     * @param imagen the imagen to set
     */
    public void setImagen(BufferedImage imagen) {
        this.imagen = imagen;
    }

    /**
     * @return the imagenURL
     */
    public String getImagenURL() {
        return imagenURL;
    }

    /**
     * @param imagenURL the imagenURL to set
     */
    public void setImagenURL(String imagenURL) {
        this.imagenURL = imagenURL;
    }


    /**
     * @return the csvURL
     */
    public String getCsvURL() {
        return csvURL;
    }

    /**
     * @param csvURL the csvURL to set
     */
    public void setCsvURL(String csvURL) {
        this.csvURL = csvURL;
    }

    /**
     * @return the anchoMapa
     */
    public int getAnchoMapa() {
        return anchoMapa;
    }

    /**
     * @param anchoMapa the anchoMapa to set
     */
    public void setAnchoMapa(int anchoMapa) {
        this.anchoMapa = anchoMapa;
    }

    /**
     * @return the altoMapa
     */
    public int getAltoMapa() {
        return altoMapa;
    }

    /**
     * @param altoMapa the altoMapa to set
     */
    public void setAltoMapa(int altoMapa) {
        this.altoMapa = altoMapa;
    }

    /**
     * @return the anchoTile
     */
    public int getAnchoTile() {
        return anchoTile;
    }

    /**
     * @param anchoTile the anchoTile to set
     */
    public void setAnchoTile(int anchoTile) {
        this.anchoTile = anchoTile;
    }

    /**
     * @return the altoTile
     */
    public int getAltoTile() {
        return altoTile;
    }

    /**
     * @param altoTile the altoTile to set
     */
    public void setAltoTile(int altoTile) {
        this.altoTile = altoTile;
    }

    /**
     * @return the tiles
     */
    public ArrayList<funTile> getTiles() {
        return tiles;
    }

    /**
     * @param tiles the tiles to set
     */
    public void setTiles(ArrayList<funTile> tiles) {
        this.tiles = tiles;
    }

    /**
     * @return the tilesImgs
     */
    public ArrayList<BufferedImage> getTilesImgs() {
        return tilesImgs;
    }

    /**
     * @param tilesImgs the tilesImgs to set
     */
    public void setTilesImgs(ArrayList<BufferedImage> tilesImgs) {
        this.tilesImgs = tilesImgs;
    }
    
}

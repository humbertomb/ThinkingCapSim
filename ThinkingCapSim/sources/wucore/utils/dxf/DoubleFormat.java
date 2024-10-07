package wucore.utils.dxf;

/**
 * @author Jose Antonio Marin
 *
 * Formatea un Double a un String, limitando el numero de decimales (con y sin redondeo)
 */
public class DoubleFormat {
    
    
    /** Convierte un double a String con 4 decimales como maximo
     * @param data	Valor double a convertir
     * @return String con la el double convertido
     */
    public static String format(double data){
        return format(data,4);
    }
    
    
    /** Convierte un double a String con un determinado numero maximo de decimales con redondeo
     * @param data	Valor double a convertir
     * @param dec	Numero de decimales
     * @return String del double convertido
     */
    public static String format(double data, int dec){
        double round = 0.5 / Math.pow(10,dec);	// Para redondear
        String str;
        if(data<0)	str = Double.toString(data-round);
        else		str = Double.toString(data+round);
        if(str.indexOf('E') != -1){
            if(data < round) return "0.0";
            return Double.toString(data);
        }
    	int index = str.lastIndexOf('.');
    	index = Math.min(str.length(), index+1+dec);
    	return Double.toString(Double.parseDouble(str.substring(0,index)));
    }
    
    /** Convierte un double a String con un determinado numero maximo de decimales sin redondeo
     * @param data	Valor double a convertir
     * @param dec	Numero de decimales
     * @return String del double convertido
     */
    public static String format1(double data, int dec){
        String str = Double.toString(data);
        if(str.indexOf('E') != -1){
            double round = 0.5 / Math.pow(10,dec);
            if(data < round) return "0.0";
            return Double.toString(data);
        }    	int index = str.lastIndexOf('.');
    	index = Math.min(str.length(), index+1+dec);
    	return Double.toString(Double.parseDouble(str.substring(0,index)));
    }
    
}

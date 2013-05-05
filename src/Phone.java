import java.rmi.activation.UnknownObjectException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.sun.media.sound.InvalidFormatException;


public class Phone {
	
	private boolean fax=false;
	private boolean home=false;
	private boolean cell=false;
	private boolean work=false;

	public Phone(String content) throws UnknownObjectException, InvalidFormatException {
		if (!content.startsWith("TEL;")) throw new InvalidFormatException("Phone does not start with \"TEL;\"");
		String line=content.substring(4);
		String upper=line.toUpperCase();
		while(!line.startsWith(":")){
			if (upper.startsWith("TYPE=FAX")){
				fax=true;
				line=line.substring(8);
				upper=line.toUpperCase();
				continue;
			}
			if (upper.startsWith("TYPE=HOME")){
				home=true;
				line=line.substring(9);
				upper=line.toUpperCase();
				continue;
			}
			if (upper.startsWith("TYPE=CELL")){
				cell=true;
				line=line.substring(9);
				upper=line.toUpperCase();
				continue;
			}
			if (upper.startsWith("\\,CELL")){
				cell=true;
				line=line.substring(6);
				upper=line.toUpperCase();
				continue;
			}
			if (upper.startsWith("TYPE=WORK")){
				work=true;
				line=line.substring(9);
				upper=line.toUpperCase();
				continue;
			}
			if (line.startsWith(";")){
				line=line.substring(1);
				upper=line.toUpperCase();
				continue;
			}
			throw new UnknownObjectException(line+" in "+content);
		}
		readPhone(line.substring(1));		
	}

	private void readPhone(String line) {
		if (line.isEmpty())return;
		line=line.replace(" ", "");
		for (char c:line.toCharArray()){
			if (!Character.isDigit(c) && c!='+') {
				System.err.println(line);
				throw new NotImplementedException();				
			}
		}
		String number = line;
	}
}
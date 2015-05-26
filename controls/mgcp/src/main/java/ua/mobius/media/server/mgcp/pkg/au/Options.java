/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.mgcp.pkg.au;

import java.util.Collection;
import ua.mobius.media.server.utils.Text;

import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * Represents parameters supplied with command.
 * 
 * @author oifa yulian
 */
public class Options {
	public static ConcurrentLinkedQueue<Options> cache = new ConcurrentLinkedQueue<Options>();
    
    private final static Text ann = new Text("an");
    private final static Text du = new Text("du");
    private final static Text of = new Text("of");
    private final static Text it = new Text("it");
    private final static Text ip = new Text("ip");
    private final static Text rp = new Text("rp");    
    private final static Text iv = new Text("iv");
    private final static Text mn = new Text("mn");
    private final static Text mx = new Text("mx");
    private final static Text dp = new Text("dp");
    private final static Text ni = new Text("ni");
    private final static Text ri = new Text("ri");
    private final static Text rlt = new Text("rlt");
    private final static Text oa = new Text("oa");
    private final static Text nd = new Text("nd");
    private final static Text ns = new Text("ns");
    private final static Text fa = new Text("fa");
    private final static Text sa = new Text("sa");
    private final static Text prt = new Text("prt");
    private final static Text pst = new Text("pst");
    private final static Text cb = new Text("cb");
    private final static Text fdt= new Text("fdt");
    private final static Text idt= new Text("idt");    
    private final static Text na= new Text("na");
    private final static Text eik = new Text("eik");
    private final static Text iek = new Text("iek");
    private final static Text psk = new Text("psk");
    private final static Text fst = new Text("fst");
    private final static Text lst = new Text("lst");
    private final static Text prv = new Text("prv");
    private final static Text nxt = new Text("nxt");
    private final static Text cur = new Text("cur");
    private final static Text dpa = new Text("dpa");
    private final static Text x_md= new Text("x-md");
    
    private final static Text TRUE = new Text("true");
    private final static Text FALSE = new Text("false");
    
    //private Text prompt = new Text(new byte[150], 0, 150);
    private Text recordID = new Text(new byte[2048], 0, 2048);
    
    private boolean isPrompt,isReprompt,isDeletePersistentAudio=false,isFailureAnnouncement=false,isSuccessAnnouncement=false,isNoSpeechReprompt=false,isNoDigitsReprompt=false;
    private boolean override = true;
    
    private Text segmentsBuffer = new Text(new byte[2048], 0, 2048);
    private Text promptBuffer = new Text(new byte[2048], 0, 2048);
    private Text repromptBuffer = new Text(new byte[2048], 0, 2048);
    private Text failureAnnouncementBuffer = new Text(new byte[2048], 0, 2048);
    private Text successAnnouncementBuffer = new Text(new byte[2048], 0, 2048);
    private Text noSpeechRepromptBuffer = new Text(new byte[2048], 0, 2048);
    private Text noDigitsRepromptBuffer = new Text(new byte[2048], 0, 2048);
    private Text deletePersistentAudioBuffer = new Text(new byte[2048], 0, 2048);
    
    private Collection<Text> segments;
    private Collection<Text> prompt;
    private Collection<Text> reprompt;
    private Collection<Text> failureAnnouncement;
    private Collection<Text> successAnnouncement;
    private Collection<Text> noSpeechReprompt;
    private Collection<Text> noDigitsReprompt;
    private Collection<Text> deletePersistentAudio;
    
    private int cursor;
    
    //max duration in milliseconds
    private long duration = -1;
    
    //intial offset in milliseconds
    private long offset = 0;
    
    //repeat count
    private int repeatCount;
    
    private long interval;
    
    private int digitsNumber,maxDigitsNumber;
    private long postSpeechTimer = -1,preSpeechTimer = -1;
    
    private Text digitPattern = new Text(new byte[2048], 0, 2048);
    private Collection digitPatterns;
    
    private Text name = new Text();
    private Text value = new Text();
    
    private Text[] parameter = new Text[]{name, value};
    
    private boolean nonInterruptable = false;
    private long recordDuration = -1;
    private boolean clearDigits = false;
    private boolean includeEndInput = false;
    
    private char endInputKey='#';
    
    private long firstDigitTimer=0;
    private long interDigitTimer=0;
    private int maxDuration=0;
    private int numberOfAttempts=0;
    
    private Text tempSequence;
    private char tempChar;
    private boolean hasNextKey=false;
    private char nextKey=' ';
    private boolean hasPrevKey=false;
    private char prevKey=' ';
    private boolean hasFirstKey=false;
    private char firstKey=' ';
    private boolean hasLastKey=false;
    private char lastKey=' ';
    private boolean hasCurrKey=false;
    private char currKey=' ';
    
    static
    {
    	for(int i=0;i<100;i++)
    		cache.offer(new Options(null));
    }
    
    public static Options allocate(Text options)
    {
    	Options currOptions=cache.poll();
    	
    	if(currOptions==null)
    		currOptions=new Options(options);
    	else
    		currOptions.init(options);
    	
    	return currOptions;
    }
    
    public static void recycle(Options options)
    {
    	options.isPrompt=false;
    	options.isReprompt=false;
    	options.isDeletePersistentAudio=false;
    	options.isFailureAnnouncement=false;
    	options.isSuccessAnnouncement=false;
    	options.isNoSpeechReprompt=false;
    	options.isNoDigitsReprompt=false;
    	options.override = true;
        
    	options.segments=null;
    	options.prompt=null;
    	options.reprompt=null;
    	options.failureAnnouncement=null;
    	options.successAnnouncement=null;
    	options.noSpeechReprompt=null;
    	options.noDigitsReprompt=null;
    	options.deletePersistentAudio=null;
        
    	options.cursor=0;        
        options.duration = -1;
        options.offset = 0;
        options.repeatCount=0;
        options.interval=0;
        options.digitsNumber=0;
        options.maxDigitsNumber=0;
        options.postSpeechTimer = -1;
        options.preSpeechTimer = -1;
        
        options.digitPatterns=null;
        
        options.nonInterruptable = false;
        options.recordDuration = -1;
        options.clearDigits = false;
        options.includeEndInput = false;
        
        options.endInputKey='#';
        
        options.firstDigitTimer=0;
        options.interDigitTimer=0;
        options.maxDuration=0;
        options.numberOfAttempts=0;
        
        options.hasNextKey=false;
        options.nextKey=' ';
        options.hasPrevKey=false;
        options.prevKey=' ';
        options.hasFirstKey=false;
        options.firstKey=' ';
        options.hasLastKey=false;
        options.lastKey=' ';
        options.hasCurrKey=false;
        options.currKey=' ';
        
    	cache.offer(options);
    }
    
    /**
     * Creates options.
     * 
     * @param options the text representation of options.
     */
    private Options(Text options) {
    	init(options);
    }
    
    private void init(Text options) {
        if (options == null || options.length() == 0) {
            return;
        }
        
        Collection<Text> params = options.split(' ');
        int count;
        
        for (Text param : params) {
            param.trim();            
            count=param.divide('=', parameter);
            
            if(count==2)
            {
            	if(name.length()==2)
            	{
            		switch(name.charAt(0))
    	            {
    	            	case 'a':
    	            	case 'A':
    	            		if(name.charAt(1)=='n' || name.charAt(1)=='N')
    	            		{
    	            			value.duplicate(segmentsBuffer);
    	                        this.segments = segmentsBuffer.split(';');
    	            		}
    	            		break;
    	            	case 'd':
    	            	case 'D':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'u':
    	            			case 'U':
    	            				this.duration = value.toInteger() * 1000000L;            		            
    	            				break;
    	            			case 'p':
    	            			case 'P':
    	            				value.duplicate(digitPattern);
	            		            digitPatterns = digitPattern.split('|'); 
    	            				break;
    	            		}
    	            		break;
    	            	case 'o':
    	            	case 'O':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'f':
    	            			case 'F':
    	            				this.offset = value.toInteger() * 1000000L;            	            
    	            				break;
    	            			case 'a':
    	            			case 'A':
    	            				this.override = value.equals(TRUE);            		            
    	            				break;
    	            		}
    	            		break;
    	            	case 'i':
    	            	case 'I':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 't':
    	            			case 'T':
    	            				this.repeatCount = value.toInteger();            		            
    	            				break;
    	            			case 'p':
    	            			case 'P':
    	            				value.duplicate(promptBuffer);
    	            				this.prompt = promptBuffer.split(';');
    	            		        this.isPrompt = true;
    	            		        break;
    	            			case 'v':
    	            			case 'V':
    	            				this.interval = value.toInteger() * 1000000L;            		            
    	            				break;    	            			
    	            		}
    	            		break;
    	            	case 'r':
    	            	case 'R':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'p':
    	            			case 'P':
    	            				value.duplicate(repromptBuffer);
    	            				this.reprompt = repromptBuffer.split(';');
    	            		        this.isReprompt = true;
    	            		        break;
    	            			case 'i':
    	            			case 'I':
    	            				value.duplicate(recordID);            		                            		             
    	            				break;    	            			
    	            		}
    	            		break;
    	            	case 'm':
    	            	case 'M':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'n':
    	            			case 'N':
    	            				this.digitsNumber = value.toInteger();            		            
    	            				break;
    	            			case 'x':
    	            			case 'X':
    	            				this.maxDigitsNumber = value.toInteger();
    	            				break;
    	            		}
    	            		break;
    	            	case 'n':
    	            	case 'N':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'i':
    	            			case 'I':
    	            				this.nonInterruptable = value.equals(TRUE); 
    	            				break;
    	            			case 'd':
    	            			case 'D':
    	            				value.duplicate(noDigitsRepromptBuffer);
    	            				this.noDigitsReprompt = noDigitsRepromptBuffer.split(';');
    	        		            this.isNoDigitsReprompt = true;                
    	        		            break;
    	            			case 'a':
    	            			case 'A':
    	            				this.numberOfAttempts = value.toInteger();            		            
    	            				break;
    	            			case 's':
    	            			case 'S':
    	            				value.duplicate(noSpeechRepromptBuffer);
    	            				this.noSpeechReprompt = noSpeechRepromptBuffer.split(';');
    	        		            this.isNoSpeechReprompt = true;                
    	        		            break;            			
    	            		}
    	            		break;
    	            	case 'f':
    	            	case 'F':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'a':
    	            			case 'A':
    	            				value.duplicate(failureAnnouncementBuffer);
    	            				this.failureAnnouncement = failureAnnouncementBuffer.split(';');
    	            				this.isFailureAnnouncement = true;                
    	            				break;    	            			
    	            		}
    	            		break;
    	            	case 's':
    	            	case 'S':
    	            		if(name.charAt(1)=='a' || name.charAt(1)=='A') {
    	            			value.duplicate(successAnnouncementBuffer);
    	            			this.successAnnouncement = successAnnouncementBuffer.split(';');
    	                    	this.isSuccessAnnouncement = true;                
    	                    } 
    	            		break;
    	            	case 'c':
    	            	case 'C':
    	            		if(name.charAt(1)=='b' || name.charAt(1)=='B')
    	                        this.clearDigits = value.equals(TRUE);                    
    	            		break;    	            	
    	            }  	
            	}  
            	else if(name.length()==3)
            	{
            		switch(name.charAt(0))
    	            {
    	            	case 'd':
    	            	case 'D':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'p':
    	            			case 'P':
    	            				if(name.charAt(2)=='a' || name.charAt(2)=='A') {
    	            					value.duplicate(deletePersistentAudioBuffer);
    	            		            this.deletePersistentAudio = deletePersistentAudioBuffer.split(';');
    	            		            this.isDeletePersistentAudio = true;                
    	            		        } 
    	            				break;
    	            		}
    	            		break;
    	            	 case 'i':
    	            	 case 'I':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'd':
    	            			case 'D':
    	            				if(name.charAt(2)=='t' || name.charAt(2)=='T')
    	            					this.interDigitTimer = value.toInteger();            		            
    	            				break;
    	            			case 'e':
    	            			case 'E':
    	            				if(name.charAt(2)=='k' || name.charAt(2)=='K')
    	            					this.includeEndInput = value.equals(TRUE);            		            
    	                			break;
    	            		}
    	            		break;
    	            	case 'r':
    	            	case 'R':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'l':
    	            			case 'L':
    	            				if(name.charAt(2)=='t' || name.charAt(2)=='T')
    	            					this.recordDuration = value.toInteger() * 1000000L;
    	            				break;
    	            		}
    	            		break;
    	            	case 'f':
    	            	case 'F':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 'd':
    	            			case 'D':
    	            				if(name.charAt(2)=='t' || name.charAt(2)=='T')
    	            					this.firstDigitTimer = value.toInteger();            		            
    	            				break;
    	            		}
    	            		break;
    	            	case 'p':
    	            	case 'P':
    	            		switch(name.charAt(1))
    	            		{
    	            			case 's':
    	            			case 'S':
    	            				switch(name.charAt(2))
    	                    		{
    	            					case 't':
    	            					case 'T':
    	            						this.postSpeechTimer = value.toInteger() * 100000000L; 
    	            						break;
    	            					case 'k':
    	            					case 'K':
    	            						if(value.length()==5) {
    	            							tempChar=value.charAt(0);
    	            			            	tempSequence=(Text)value.subSequence(2,5);
    	            			            	switch(tempSequence.charAt(0))
    	            			            	{
    	            			            		case 'f':
    	            			            		case 'F':
    	            			            			if (tempSequence.equals(fst))
    	                    			            	{
    	                    			            		this.firstKey=tempChar;
    	                    			            		this.hasFirstKey=true;
    	                    			            	}   
    	            			            			break;
    	            			            		case 'l':
    	            			            		case 'L':
    	            			            			if (tempSequence.equals(lst))
    	                    			            	{
    	                    			            		this.lastKey=tempChar;
    	                    			            		this.hasLastKey=true;
    	                    			            	}
    	            			            			break;
    	            			            		case 'n':
    	            			            		case 'N':
    	            			            			if (tempSequence.equals(nxt))
    	                    			                {
    	                    			                   	this.nextKey=tempChar;
    	                    			                   	this.hasNextKey=true;
    	                    			                }
    	            			            			break;
    	            			            		case 'c':
    	            			            		case 'C':
    	            			            			if (tempSequence.equals(cur))
    	                    			                {
    	                    			                   	this.currKey=tempChar;
    	                    			                   	this.hasCurrKey=true;
    	                    			                }
    	            			            			break;
    	            			            		case 'p':
    	            			            		case 'P':
    	            			            			if (tempSequence.equals(prv))
    	                    			                {
    	                    			                	this.prevKey=tempChar;
    	                    			                	this.hasPrevKey=true;
    	                    			                }
    	            			            			break;
    	            			            	}            			            	 
    	            						}
    	            						break;
    	                    		}
    	            				break;
    	            			case 'r':
    	            			case 'R':
    	            				if(name.charAt(2)=='t' || name.charAt(2)=='T')
    	            					this.preSpeechTimer = value.toInteger() * 100000000L;            		            
    	            				break;
    	            		}
    	            		break;
    	            	case 'c':
    	            	case 'C':
    	            		if(name.charAt(1)=='b' || name.charAt(1)=='B')
    	                        this.clearDigits = value.equals(TRUE);                    
    	            		break;
    	            	case 'e':
    	            	case 'E':
    	            		if (name.equals(eik) && value.length()==1)
    	                        this.endInputKey = value.charAt(0);                    
    	            		break;    	            	
    	            }  
            	}
            	else if (name.equals(x_md))
                    this.maxDuration = value.toInteger();        		
            }
        }
    }

    public Collection<Text> getSegments() {
        return segments;
    }
    
    public boolean hasPrompt() {
        return this.isPrompt;
    }
    
    public Collection<Text> getPrompt() {
        return prompt;
    }
    
    public boolean hasReprompt() {
        return this.isReprompt;
    }
    
    public Collection<Text> getReprompt() {
        return reprompt;
    }
    
    public boolean hasDeletePresistentAudio() {
        return this.isDeletePersistentAudio;
    }
    
    public Collection<Text> getDeletePersistentAudio() {
        return this.deletePersistentAudio;
    }
    
    public boolean hasNoSpeechReprompt() {
        return this.isNoSpeechReprompt;
    }
    
    public Collection<Text> getNoSpeechReprompt() {
        return this.noSpeechReprompt;
    }
    
    public boolean hasNoDigitsReprompt() {
        return this.isNoDigitsReprompt;
    }
    
    public Collection<Text> getNoDigitsReprompt() {
        return this.noDigitsReprompt;
    }
    
    public boolean hasSuccessAnnouncement() {
        return this.isSuccessAnnouncement;
    }
    
    public Collection<Text> getSuccessAnnouncement() {
        return this.successAnnouncement;
    }
    
    public boolean hasFailureAnnouncement() {
        return this.isFailureAnnouncement;
    }
    
    public Collection<Text> getFailureAnnouncement() {
        return this.failureAnnouncement;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public long getOffset() {
        return offset;
    }
    
    public int getRepeatCount() {
        return repeatCount;
    }
    
    public long getInterval() {
        return interval;
    }
    
    public int getDigitsNumber() {
        return this.digitsNumber;
    }
    
    public int getMaxDigitsNumber() {
        return this.maxDigitsNumber;
    }
    
    public Collection<Text> getDigitPattern() {
        return digitPatterns;
    }
    
    public boolean isNonInterruptable() {
        return this.nonInterruptable;
    }
    
    public Text getRecordID() {
        return this.recordID;
    }
    
    public long getRecordDuration() {
        return this.recordDuration;
    }
    
    public boolean isOverride() {
        return this.override;
    }
    
    public long getPostSpeechTimer() {
        return this.postSpeechTimer;
    }
    
    public long getPreSpeechTimer() {
        return this.preSpeechTimer;
    }
    
    public long getFirstDigitTimer() {
        return this.firstDigitTimer;
    }
    
    public long getInterDigitTimer() {
        return this.interDigitTimer;
    }
    
    public int getMaxDuration() {
        return this.maxDuration;
    }
    
    public char getEndInputKey() {
        return this.endInputKey;
    }
    
    public int getNumberOfAttempts() {
        return this.numberOfAttempts;
    }    
    
    public boolean isClearDigits() {
        return clearDigits;
    }
    
    public boolean isIncludeEndInputKey() {
        return includeEndInput;
    }
    
    public boolean prevKeyValid() {
        return this.hasPrevKey;
    }
    
    public char getPrevKey() {
        return this.prevKey;
    }
    
    public boolean firstKeyValid() {
        return this.hasFirstKey;
    }
    
    public char getFirstKey() {
        return this.firstKey;
    }
    
    public boolean currKeyValid() {
        return this.hasCurrKey;
    }
    
    public char getCurrKey() {
        return this.currKey;
    }
    
    public boolean nextKeyValid() {
        return this.hasNextKey;
    }
    
    public char getNextKey() {
        return this.nextKey;
    }
    
    public boolean lastKeyValid() {
        return this.hasLastKey;
    }
    
    public char getLastKey() {
        return this.lastKey;
    }
}

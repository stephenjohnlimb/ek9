package org.ek9lang.core.exception;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

import org.ek9lang.core.utils.OsSupport;

/**
 * Used as simple one liners to check a value and issue an illegal argument exception if empty.
 */
public class AssertValue
{
	private static OsSupport osSupport = new OsSupport();
	
	/**
	 * Checks if a string value is null or an empty string and if so issues an illegal argument exception.
	 * @param valueToCheck The value to check.
	 */
	public static void checkNotEmpty(String messageIfEmpty, String valueToCheck)
	{
		if(valueToCheck == null || "".equals(valueToCheck))
			throw new IllegalArgumentException(messageIfEmpty);
	}
	
	public static void checkNotEmpty(String messageIfEmpty, String[] valuesToCheck)
	{
		if(valuesToCheck == null || valuesToCheck.length == 0)
			throw new IllegalArgumentException(messageIfEmpty);
		for(String value : valuesToCheck)
			AssertValue.checkNotEmpty(messageIfEmpty, value);
	}
	
	public static void checkRange(String messageIfOutside, Integer valueToCheck, Integer min, Integer max)
	{
		if(valueToCheck == null)
			throw new IllegalArgumentException(messageIfOutside);
		if(min != null && valueToCheck < min)
			throw new IllegalArgumentException(messageIfOutside);
		if(max != null && valueToCheck > max)
			throw new IllegalArgumentException(messageIfOutside);
	}
	
	public static void checkNotNull(String messageIfNull, Object valueToCheck)
	{
		if(valueToCheck == null)
			throw new IllegalArgumentException(messageIfNull);
	}
	
	public static void checkCanReadFile(String messageIfNoRead, String fileName)
	{
		checkNotEmpty("Filename cannot be empty or null", fileName);
		if(!osSupport.isFileReadable(fileName))
			throw new IllegalArgumentException(messageIfNoRead + "[" + fileName + "]");
	}
	
	public static void checkDirectoryReadable(String messageIfNoRead, String directoryName)
	{
		checkNotEmpty("Filename cannot be empty or null", directoryName);
		if(!osSupport.isDirectoryReadable(directoryName))
			throw new IllegalArgumentException(messageIfNoRead + "[" + directoryName + "]");
	}

	public static void checkDirectoryWritable(String messageIfNoWrite, String directoryName)
	{
		AssertValue.checkNotNull(messageIfNoWrite, directoryName);
		if(!osSupport.isDirectoryWritable(directoryName))
			throw new IllegalArgumentException(messageIfNoWrite + "[" + directoryName + "]");		
	}

	public static void checkDirectoryWritable(String messageIfNoWrite, File dir)
	{
		AssertValue.checkNotNull(messageIfNoWrite, dir);
		if(!osSupport.isDirectoryWritable(dir))
			throw new IllegalArgumentException(messageIfNoWrite + "[" + dir.getPath() + "]");
	}

	public static void checkNotEmpty(String messageIfEmpty, Optional<?> item)
	{
		checkNotNull(messageIfEmpty, item);
		if(!item.isPresent())
			throw new IllegalArgumentException(messageIfEmpty);
		
	}
	public static void checkNotEmpty(String messageIfEmpty, Collection<?> items)
	{
		checkNotNull(messageIfEmpty, items);
		if(items.size() == 0)
			throw new IllegalArgumentException(messageIfEmpty);
		items.forEach(item -> checkNotNull(messageIfEmpty, item));
	}

	public static void checkTrue(String messageIfFalse, boolean value)
	{
		if(!value)
			throw new IllegalArgumentException(messageIfFalse);
	}
}

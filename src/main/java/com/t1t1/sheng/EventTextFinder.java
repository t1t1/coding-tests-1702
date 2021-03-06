package com.t1t1.sheng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author t1t1
 * 
 * 문제 해결 방법: 
 * 
 * 성능 최적화를 하지 못한 채 전체를 탐색하는 방법으로 구현하였습니다.
 * EventText일 수 있는 문구와 해당 문구의 count 데이터를 맵으로 저장한 후
 * Map < count, Set < eventText> > 형태로 최종 가공하여
 * 내림차순(count가 크고, eventText가 긴)으로 문구가 EventText 조건에 부합하는지 확인했습니다.
 * 
 * 최초에는 전체 탐색이 아닌 EventText char 길이가 작은 것부터
 * 탐색과 함께 빈도율을 계산하여 빈도율이 높은 상품명만 다시 탐색해 나가고,
 * 빈도율이 하락하는 지점을 포착하여 이전 문구를 EventText로 판단하는 방식으로 구현하고 싶었으나,
 * 코드 구현간 Call by reference 하는 로직에서 데이터가 꼬이는 등의 문제가 발생하고
 * EventText char 길이가 큰 것에서 작을 것으로 되돌아가 탐색하는 부분에서 막혀 구현하지 못했습니다.
 *
 */
public class EventTextFinder {
	
	private List<String> data; // 상품명 리스트
	
	/**
	 * @return
	 */
	public String execute() {
		
		// 상품명 줄 단위 List 형태로 가져오기
		data = parseTxtToList("src/main/resources/text/data.txt");
		
		// 모든 상품명에 대하여 Event Text 일 수 있는 모든 문구들을 카운트하여 맵에 저장
		Map<String, Integer> stringMap = getStringMap(data);
		
		// 출현 빈도를 많은 순으로 탐색하기 위해 stringMap을 count를 key로 사용하는 Map으로 재가공
		LinkedHashMap<Integer, LinkedHashSet<String>> countMap = getCountMap(stringMap);
		
		return findEventText(countMap);
	}
	
	/**
	 * @param filePath 파일경로
	 * @return
	 * 
	 * txt 파일을 읽어 줄 단위로 List 에 담음
	 * 
	 */
	private List<String> parseTxtToList(String filePath) {
		
		List<String> textList = new ArrayList<String>();
		
		BufferedReader br = null;
		FileReader fr = null;
		
		try {
			File data = new File(filePath); // TODO 윈도우 외 OS 확인
			fr = new FileReader(data);
			br = new BufferedReader(fr);
			String line;
			while ( (line = br.readLine()) != null) {
				textList.add(line);
				if (line.isEmpty()) break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) br.close();
				if (fr != null) fr.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return textList;
	}

	/**
	 * @param data 상품명
	 * @return Map < EventText, count >
	 * 
	 * 상품명에서 EventText일 수 있는 문구를 count하여 Map에 저장하여 반환
	 * 
	 */
	private Map<String, Integer> getStringMap(List<String> data) {
		Map<String, Integer> stringMap = new LinkedHashMap();
		
		// Event Text 길이 조건
		int eventTextMinLength = 2;
		int eventTextMaxLength = 20;
		
		for (int i = eventTextMinLength; i < eventTextMaxLength + 1; i++) {
			
			for (int k = 0; k < data.size(); k++) {
				
				String line = data.get(k);
				
				// 길이 체크
				if (line.length() <= i) {
					continue;
				}
				
				String eventText = line.substring(0, i);
				
				Integer oldValue = stringMap.get(eventText);
				stringMap.put(eventText, oldValue == null ? 1 : oldValue + 1);
			}
			
		}
		return stringMap;
	};

	/**
	 * @param stringMap Map < EventText, count >
	 * @return Map < count, Set < EventText > >
	 * 
	 * count 를 key 로 pivot 함
	 */
	private LinkedHashMap<Integer, LinkedHashSet<String>> getCountMap(Map<String, Integer> stringMap) {
		LinkedHashMap<Integer, LinkedHashSet<String>> countMap = new LinkedHashMap();
		Set<String> stringMapKeys = stringMap.keySet();
		for (String stringMapKey : stringMapKeys) {
			int count = stringMap.get(stringMapKey);
			LinkedHashSet<String> eventTextSet = countMap.get(count);
			if (eventTextSet == null) {
				eventTextSet = new LinkedHashSet<String>();
			}
			eventTextSet.add(stringMapKey);
			countMap.put(count, eventTextSet);
		}
		return countMap;
	}

	/**
	 * @param countMap Map < count, Set < EventText > >
	 * @return
	 * 
	 * count 가 높은 순부터 탐색, 
	 * EventText 가 긴 순부터 탐색하여
	 * EventText 조건(특수문자 포함)을 만족하면 해당값을 EventText로 선택한다
	 * 
	 */
	private String findEventText(LinkedHashMap<Integer, LinkedHashSet<String>> countMap) {
		String maxEventText = null;
		int maxEventTextRate = 0;
		
		Set<Integer> countMapKeys = countMap.keySet();
		int maxCount = 0;
		LinkedHashSet<String> maxEventTextSet = null;
		
		searchEventTextLoop:
		while (!countMapKeys.isEmpty()) {
			maxCount = Collections.max(countMapKeys);
			
			maxEventTextSet = countMap.get(maxCount);
			
			while (!maxEventTextSet.isEmpty()) {
				maxEventText = Collections.max(maxEventTextSet);
				if (isContainSpecialChar(maxEventText)) {
					maxEventTextRate = (maxCount * 100) / data.size();
					break searchEventTextLoop;
				}
				maxEventTextSet.remove(maxEventText);
			}
			
			countMapKeys.remove(maxCount);
			
		}
		
		return formatEventText(maxEventText, maxEventTextRate);
	}
	
	/**
	 * @param words 문자열
	 * @return
	 * 
	 * 특수문자 포함 여부 확인
	 * 
	 * 숫자, 영문, 한글 제외는 모두 특수문자로 판단
	 * 
	 */
	public boolean isContainSpecialChar(String words) {
		Pattern p = Pattern.compile("[^0-9a-zA-Z가-힣]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(words);
		boolean b = m.find();
		
		return b;
	}

	/**
	 * @param maxEventText Event Text 문구
	 * @param maxEventTextRate Event Text 비율 (전체 상품에 몇%)
	 * @return
	 */
	private String formatEventText(String maxEventText, int maxEventTextRate) {
		if (maxEventText == null) {
			return String.format("%s", "Event Text를 찾을 수 없습니다.");
		} else {
			return String.format("%s, %d%%", maxEventText, maxEventTextRate);
		}
	}
	
}

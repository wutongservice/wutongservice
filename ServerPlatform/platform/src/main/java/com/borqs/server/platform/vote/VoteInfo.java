package com.borqs.server.platform.vote;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang.ObjectUtils;
import org.codehaus.jackson.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class VoteInfo {
    private String title;
    private String description;
    private List<String> choices;
    private int minChoice = 1;
    private int maxChoice = 1;
    private long startTime = 0L;
    private long endTime = Long.MAX_VALUE;

    public VoteInfo() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    public int getMinChoice() {
        return minChoice;
    }

    public void setMinChoice(int minChoice) {
        this.minChoice = minChoice;
    }

    public int getMaxChoice() {
        return maxChoice;
    }

    public void setMaxChoice(int maxChoice) {
        this.maxChoice = maxChoice;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String toJson(boolean human) {
        Record rec = new Record();
        rec.put("title", ObjectUtils.toString(title));
        rec.put("description", ObjectUtils.toString(description));
        rec.put("choices", choices != null ? choices : new ArrayList<String>());
        rec.put("min_choice", minChoice);
        rec.put("max_choice", maxChoice);
        rec.put("start_time", startTime);
        rec.put("end_time", endTime);
        return rec.toString(true, human);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    public static VoteInfo fromJsonNode(JsonNode jn) {
        VoteInfo vi = new VoteInfo();
        vi.setTitle(ObjectUtils.toString(jn.path("title").getValueAsText()));
        vi.setDescription(ObjectUtils.toString(jn.path("description").getValueAsText()));
        if (jn.has("choices")) {
            JsonNode choicesNode = jn.get("choices");
            ArrayList<String> choices = new ArrayList<String>();
            for (int i = 0; i < choicesNode.size(); i++) {
                choices.add(ObjectUtils.toString(choicesNode.get(i).getValueAsText()));
            }
            vi.setChoices(choices);
        } else {
            vi.setChoices(new ArrayList<String>());
        }
        vi.setMinChoice(jn.path("min_choice").getValueAsInt(1));
        vi.setMaxChoice(jn.path("max_choice").getValueAsInt(1));
        vi.setStartTime(jn.path("start_time").getValueAsLong(0));
        vi.setEndTime(jn.path("end_time").getValueAsLong(Long.MAX_VALUE));
        return vi;
    }

    public static VoteInfo fromJson(String json) {
        return fromJsonNode(JsonUtils.parse(json));
    }

    public String getDisplayMessage() {
        return "";
    }
}

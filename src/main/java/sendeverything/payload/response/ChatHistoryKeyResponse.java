package sendeverything.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryKeyResponse {
    private Map<Integer, String> sharedKeys;
    private List<Integer> userCounts;
}

package capstone.fridge.domain.fridge.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class FridgeBatchDtos {

    @Getter @Setter
    public static class BatchAddReq {
        private List<FridgeDtos.AddIngredientReq> items = new ArrayList<>();
    }

    @Getter @Setter
    public static class BatchAddRes {
        private List<FridgeDtos.IngredientRes> saved = new ArrayList<>();

        public BatchAddRes() {}

        public BatchAddRes(List<FridgeDtos.IngredientRes> saved) {
            this.saved = saved;
        }
    }
}

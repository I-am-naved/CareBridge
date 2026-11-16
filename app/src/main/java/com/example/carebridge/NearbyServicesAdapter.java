package com.example.carebridge;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class NearbyServicesAdapter extends RecyclerView.Adapter<NearbyServicesAdapter.ViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(NearbyPlace place);
    }

    private final Context context;
    private List<NearbyPlace> placeList;
    private OnPlaceClickListener listener;

    public NearbyServicesAdapter(Context context, List<NearbyPlace> placeList) {
        this(context, placeList, null);
    }

    public NearbyServicesAdapter(Context context, List<NearbyPlace> placeList, OnPlaceClickListener listener) {
        this.context = context;
        this.placeList = placeList;
        this.listener = listener;
    }

    public void setOnPlaceClickListener(OnPlaceClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<NearbyPlace> newList) {
        placeList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyPlace place = placeList.get(position);
        holder.nameText.setText(place.getName());
        holder.addressText.setText(place.getAddress());
        holder.distanceText.setText(String.format(Locale.getDefault(), "%.1f km away", place.getDistanceKm()));
        holder.categoryText.setText(categoryLabel(place.getCategory()));

        if ("hospital".equalsIgnoreCase(place.getCategory())) {
            holder.categoryBadge.setText("H");
            holder.categoryBadge.setBackgroundResource(R.drawable.bg_nearby_badge_blue);
        } else if ("pharmacy".equalsIgnoreCase(place.getCategory())) {
            holder.categoryBadge.setText("Rx");
            holder.categoryBadge.setBackgroundResource(R.drawable.bg_nearby_badge_purple);
        } else if ("laboratory".equalsIgnoreCase(place.getCategory())) {
            holder.categoryBadge.setText("Lab");
            holder.categoryBadge.setBackgroundResource(R.drawable.bg_nearby_badge_purple);
        } else {
            holder.categoryBadge.setText("+");
            holder.categoryBadge.setBackgroundResource(R.drawable.bg_nearby_badge_teal);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaceClick(place);
            }
        });
    }

    private String categoryLabel(String category) {
        if ("hospital".equalsIgnoreCase(category)) return "HOSPITAL";
        if ("pharmacy".equalsIgnoreCase(category)) return "PHARMACY";
        if ("laboratory".equalsIgnoreCase(category)) return "LABORATORY";
        return "CLINIC";
    }

    @Override
    public int getItemCount() {
        return placeList == null ? 0 : placeList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryBadge, nameText, addressText, distanceText, categoryText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryBadge = itemView.findViewById(R.id.placeCategoryBadge);
            nameText = itemView.findViewById(R.id.placeName);
            addressText = itemView.findViewById(R.id.placeAddress);
            distanceText = itemView.findViewById(R.id.placeDistance);
            categoryText = itemView.findViewById(R.id.placeCategory);
        }
    }
}

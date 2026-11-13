package com.example.carebridge;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class NearbyServicesAdapter extends RecyclerView.Adapter<NearbyServicesAdapter.ViewHolder> {

    private final Context context;
    private List<NearbyPlace> placeList;

    public NearbyServicesAdapter(Context context, List<NearbyPlace> placeList) {
        this.context = context;
        this.placeList = placeList;
    }

    public void updateList(List<NearbyPlace> newList) {
        this.placeList = newList;
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
        holder.distanceText.setText(String.format(Locale.US, "📍 %.1f km", place.getDistanceKm()));

        if ("hospital".equalsIgnoreCase(place.getCategory())) {
            holder.categoryBadge.setText("H");
            holder.categoryBadge.setBackgroundColor(0xFFE0F2FE);
            holder.categoryBadge.setTextColor(0xFF0284C7);
        } else if ("pharmacy".equalsIgnoreCase(place.getCategory())) {
            holder.categoryBadge.setText("💊");
            holder.categoryBadge.setBackgroundColor(0xFFF3E8FF);
            holder.categoryBadge.setTextColor(0xFF9333EA);
        } else { // clinic
            holder.categoryBadge.setText("✚");
            holder.categoryBadge.setBackgroundColor(0xFFDCFCE7);
            holder.categoryBadge.setTextColor(0xFF16A34A);
        }

        holder.itemView.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + place.getLatitude() + "," + place.getLongitude());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
            } else {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + place.getLatitude() + "," + place.getLongitude()));
                context.startActivity(webIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return placeList != null ? placeList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryBadge, nameText, addressText, distanceText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryBadge = itemView.findViewById(R.id.placeCategoryBadge);
            nameText = itemView.findViewById(R.id.placeName);
            addressText = itemView.findViewById(R.id.placeAddress);
            distanceText = itemView.findViewById(R.id.placeDistance);
        }
    }
}

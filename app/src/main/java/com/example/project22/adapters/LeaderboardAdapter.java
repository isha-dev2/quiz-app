package com.example.project22.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project22.R;
import com.example.project22.models.LeaderboardEntry;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private final List<LeaderboardEntry> leaderboardList;

    public LeaderboardAdapter(List<LeaderboardEntry> leaderboardList) {
        this.leaderboardList = leaderboardList;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        LeaderboardEntry entry = leaderboardList.get(position);
        
        // Set rank with proper suffix (1st, 2nd, 3rd, etc.)
        String rankSuffix;
        switch (entry.getRank() % 10) {
            case 1:
                rankSuffix = "st";
                break;
            case 2:
                rankSuffix = "nd";
                break;
            case 3:
                rankSuffix = "rd";
                break;
            default:
                rankSuffix = "th";
        }
        
        // Special case for 11, 12, 13
        if (entry.getRank() >= 11 && entry.getRank() <= 13) {
            rankSuffix = "th";
        }
        
        holder.tvRank.setText(String.format("%d%s", entry.getRank(), rankSuffix));
        holder.tvName.setText(entry.getName());
        // Display the raw score instead of a percentage
        holder.tvScore.setText(String.valueOf(entry.getScore()));
    }

    @Override
    public int getItemCount() {
        return leaderboardList.size();
    }

    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvName;
        TextView tvScore;

        LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvName = itemView.findViewById(R.id.tvName);
            tvScore = itemView.findViewById(R.id.tvScore);
        }
    }
}

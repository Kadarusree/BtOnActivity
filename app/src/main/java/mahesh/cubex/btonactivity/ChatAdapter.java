package mahesh.cubex.btonactivity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.ArrayList;


public class ChatAdapter extends BaseAdapter {


    ArrayList<Bitmap> mBitmap;
    Activity activity;

    public ChatAdapter(ArrayList<Bitmap> mBitmap_, Activity activty_)
    {
        this.mBitmap = mBitmap_;
        this.activity = activty_;
    }
    @Override
    public int getCount() {
        return mBitmap.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        LayoutInflater inflater = LayoutInflater.from(activity);
        View v = inflater.inflate(R.layout.image_layout,null);
        ImageView img = v.findViewById(R.id.imageView);
        img.setImageBitmap(mBitmap.get(i));
        return v;
    }
}

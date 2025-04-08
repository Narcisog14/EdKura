import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.edkura.R

class CourseAdapter(
    private var courses: List<Pair<String, String>>,  // 变为 List<Pair<String, String>>
    private val onLongClick: (Int) -> Unit,
    private val onItemClick: (Pair<String, String>) -> Unit  // 接收 Pair 类型
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val courseName: TextView = itemView.findViewById(R.id.textCourseName)
        val subject: TextView = itemView.findViewById(R.id.textsubject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.`jk_course_item`, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val (subject, course) = courses[position]  // 直接从 Pair 中提取 subject 和 course
        holder.courseName.text = course
        holder.subject.text = subject

        holder.itemView.setOnLongClickListener {
            onLongClick(position)
            true
        }

        // 直接传递 Pair 类型的课程信息
        holder.itemView.setOnClickListener {
            onItemClick(courses[position])  // 传递 Pair 类型
        }
    }

    override fun getItemCount(): Int = courses.size

    // 更新数据，重新设置 courses 为新的 List<Pair<String, String>>
    fun updateData(newCourses: List<Pair<String, String>>) {
        courses = newCourses
        notifyDataSetChanged()
    }
}
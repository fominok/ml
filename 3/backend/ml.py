import numpy as np
import pandas as pd
from matplotlib import pyplot as plt
from scipy.stats import pearsonr
from sklearn.model_selection import train_test_split
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis as LDA, QuadraticDiscriminantAnalysis
from sklearn import metrics
import sys
import io

def prepare_ds(name, class_field):
    ds = pd.read_csv(name)
    ds_attrs = ds.drop([class_field], axis=1).values
    ds_class = ds[class_field].values
    return ds_attrs, ds_class

def train_and_test(filename, class_field):
    attrs, classes = prepare_ds(filename, class_field)

    corr_scatter = []

    for cls, color in zip(range(1,4), ('red', 'green', 'blue')):
        attr_one = attrs[:, 0][classes == cls]
        attr_two = attrs[:, 1][classes == cls]
        p = pearsonr(attr_one, attr_two)
        corr_scatter.append({"x": attr_one.tolist(), "y": attr_two.tolist(), "p": p[0]})
        # plt.scatter(x=attr_one, y=attr_two, marker='o', color=color,
        #             label='cls: {:}, pearsonr={:.2f}'.format(cls, p[0]))


    # plt.title('Pearson correlation')
    # plt.xlabel('Elevation, m')
    # plt.ylabel('Slope, num')
    # plt.legend(loc='upper right')
    # plt.show()

    data_train, data_test, class_train, class_test = train_test_split(
        attrs,
        classes,
        test_size=.3,
        random_state=123,
    )

    lda = LDA(n_components=2)
    lda_transform = lda.fit_transform(data_train, class_train)

    lda_scatter = []
    # plt.figure(figsize=(10, 8))
    for cls, color in zip(range(1,4), ('red', 'green', 'blue')):
        attr_one = lda_transform[:, 0][class_train == cls]
        attr_two = lda_transform[:, 1][class_train == cls]
        lda_scatter.append({"x": attr_one.tolist(), "y": attr_two.tolist()})
        # plt.scatter(x=attr_one, y=attr_two, marker='o', color=color,
        #             label='cls: {:}'.format(cls))

    # plt.xlabel('vec 1')
    # plt.ylabel('vec 2')
    # plt.legend()
    # plt.show()

    lda_clf = LDA()
    lda_clf.fit(data_train, class_train)

    pred_train_lda = lda_clf.predict(data_train)
    print('Точность классификации на обучающем наборе данных (LDA): {:.2%}'.format(
        metrics.accuracy_score(class_train, pred_train_lda)))

    pred_test_lda = lda_clf.predict(data_test)
    print('Точность классификации на тестовом наборе данных (LDA): {:.2%}'.format(
        metrics.accuracy_score(class_test, pred_test_lda)))

    qda_clf = QuadraticDiscriminantAnalysis()
    qda_clf.fit(data_train, class_train)

    pred_train_qda = qda_clf.predict(data_train)
    print('Точность классификации на обучающем наборе данных (QDA): {:.2%}'.format(
        metrics.accuracy_score(class_train, pred_train_qda)))

    pred_test_qda = qda_clf.predict(data_test)
    print('Точность классификации на тестовом наборе данных (QDA): {:.2%}'.format(
        metrics.accuracy_score(class_test, pred_test_qda)
    ))
    return corr_scatter, lda_scatter

def main(filename, class_field):
    old_stdout = sys.stdout
    sys.stdout = result = io.StringIO()
    corr_scatter, lda_scatter = train_and_test(filename, class_field)
    sys.stdout = old_stdout
    value = result.getvalue()
    result.close()
    return {
        "report": value,
        "corr_scatter": corr_scatter,
        "lda_scatter": lda_scatter,
    }

# if __name__ == '__main__':
#     main()
